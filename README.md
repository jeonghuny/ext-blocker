# 파일 확장자 차단 & 업로드 검증

파일 업로드 시 확장자 기반 차단 정책을 관리하고, 그 정책이 **실제 업로드에 강제**되는 서비스입니다.

- **배포 URL:** https://ext-blocker-production-aad3.up.railway.app
- **고려사항 문서:** [CONSIDERATIONS.md](./CONSIDERATIONS.md)
- **AI 활용 기록:** [PROMPT_LOG.md](./PROMPT_LOG.md)

---

## 기술 스택

| 구분 | 사용 기술 |
|---|---|
| Backend | Java 21, Spring Boot 4.1, Spring Data JPA |
| DB | PostgreSQL 16 |
| Migration | Flyway |
| Frontend | 순수 HTML + Vanilla JS (빌드 도구 없음, jar에 내장) |
| Test | JUnit 5, AssertJ |
| Deploy | Railway (Dockerfile 기반) |

프론트엔드를 별도 빌드 없이 `src/main/resources/static`에 두어
**배포 대상을 단일 jar로 유지**했습니다.

---

## 실행 방법

### 사전 요구사항

- JDK 21
- Docker (로컬 PostgreSQL 용)

### 1. DB 실행

```bash
docker run -d --name extdb \
  -e POSTGRES_DB=extblocker \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=postgres \
  -p 5432:5432 postgres:16
```

### 2. 애플리케이션 실행

```bash
./mvnw clean spring-boot:run
```

http://localhost:8080 접속.

스키마 생성과 고정 확장자 7종 초기 데이터는 **Flyway가 자동 처리**합니다.

### 3. 테스트

```bash
./mvnw test
```

파일명 검증 단위 테스트 44건이 실행됩니다.

> `ExtBlockerApplicationTests`는 `@Disabled` 상태입니다.
> 컨텍스트 로드에 실제 DB 연결이 필요하나 빌드 환경에는 DB가 없기 때문입니다.
> 판단 근거는 CONSIDERATIONS.md 6절 참고.

### 4. 환경 변수

로컬은 기본값으로 동작하며, 배포 환경에서만 주입합니다.

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PORT` | 8080 | 서버 포트 |
| `PGHOST` | localhost | |
| `PGPORT` | 5432 | |
| `PGDATABASE` | extblocker | |
| `PGUSER` | postgres | |
| `PGPASSWORD` | postgres | |

---

## 검증 스크립트

**정책이 실제 업로드에서 강제되는지** 직접 확인할 수 있습니다.

```bash
./scripts/verify.sh                      # 로컬
./scripts/verify.sh https://배포주소      # 배포본
```

실행 결과:

```
=== exe 차단 ON ===
sample.txt (정상)          → {"result":"ACCEPTED"}
virus.exe                  → {"result":"REJECTED","error":"BLOCKED_EXTENSION","matchedExtension":"exe"}
VIRUS.EXE (대문자)          → {"result":"REJECTED","error":"BLOCKED_EXTENSION","matchedExtension":"exe"}
virus.exe. (후행 점)        → {"result":"REJECTED","error":"BLOCKED_EXTENSION","matchedExtension":"exe"}
virus.exe.txt (이중)        → {"result":"REJECTED","error":"BLOCKED_EXTENSION","matchedExtension":"exe"}
../../etc/passwd           → {"result":"REJECTED","error":"PATH_TRAVERSAL"}
disguised.jpg (MZ 헤더)     → {"result":"REJECTED","error":"EXECUTABLE_CONTENT"}

=== exe 차단 OFF — 정책 즉시 반영 확인 ===
virus.exe (해제 후)         → {"result":"ACCEPTED"}
```

마지막 두 줄이 핵심입니다.

- `disguised.jpg` — 확장자가 통과해도 **내용이 실행 파일이면 차단**
- `virus.exe (해제 후)` — **정책 변경이 업로드에 즉시 반영**

>  이 스크립트는 `exe` 차단 정책을 켰다 끕니다. 배포본에 실행하면 실제 정책이 변경됩니다.

클라이언트 검증이 우회 가능함도 재현할 수 있습니다.

```bash
curl -F "file=@sample.txt;filename=virus.exe;type=image/png" $BASE/api/upload
```

브라우저를 거치지 않으므로 클라이언트 검증이 존재하지 않지만, 서버가 거부합니다.

---

## 테이블 스키마

`src/main/resources/db/migration/V1__init.sql`

```sql
CREATE TABLE blocked_extension (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(20)  NOT NULL,
    type        VARCHAR(10)  NOT NULL,
    is_blocked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    CONSTRAINT uq_blocked_extension_name UNIQUE (name),
    CONSTRAINT ck_blocked_extension_type CHECK (type IN ('FIXED', 'CUSTOM')),
    CONSTRAINT ck_blocked_extension_name CHECK (name ~ '^[a-z0-9]{1,20}$')
);
```

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| `id` | `BIGSERIAL` | PK | |
| `name` | `VARCHAR(20)` | `UNIQUE`, `CHECK` | 소문자 정규화, 점 제외 |
| `type` | `VARCHAR(10)` | `CHECK IN ('FIXED','CUSTOM')` | 고정 / 커스텀 |
| `is_blocked` | `BOOLEAN` | `NOT NULL` | 커스텀은 추가 시 `true` |
| `created_at` | `TIMESTAMPTZ` | `DEFAULT now()` | DB 시계 사용 |
| `updated_at` | `TIMESTAMPTZ` | `DEFAULT now()` | |

**설계 의도**

- **단일 테이블 + `type` 컬럼:** 고정/커스텀을 분리하면 진실의 원천이 둘이 됩니다.
  하나로 두고 `UNIQUE (name)`을 걸면 **교차 중복이 구조적으로 불가능**합니다.
- **`UNIQUE`는 성능이 아니라 정합성 목적:** 최대 207행이므로 조회 성능용 인덱스는 무의미합니다.
  동시 추가 요청의 최종 방어선 역할입니다.
- **`CHECK` 정규식:** 애플리케이션 검증과 동일한 규칙을 DB에도 둔 2중 방어.
- **`TIMESTAMPTZ`:** 서버 타임존이 UTC임을 실측 확인. 저장 UTC / 표시 로컬.

---

## API

### 정책 관리

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/extensions` | 고정 + 커스텀 목록, 개수, 제한값 |
| `PATCH` | `/api/extensions/fixed/{name}` | 고정 확장자 차단 on/off |
| `POST` | `/api/extensions/custom` | 커스텀 추가 |
| `DELETE` | `/api/extensions/custom/{name}` | 커스텀 삭제 |

### 업로드

| Method | Path | 설명 |
|---|---|---|
| `POST` | `/api/upload` | 파일 업로드 (정책 검증) |

**검증 순서** — 비용이 낮은 검사부터 수행하여 자원 낭비를 방지합니다.

```
1. 크기        (Spring multipart, 413)
2. 파일명      (경로 조작 / 제어 문자 / 길이)
3. 확장자 정책 (DB 차단 목록 대조)
4. 시그니처    (파일 앞부분 바이트로 실행 파일 탐지)
```

**응답 예시**

```json
// 허용
{ "result": "ACCEPTED", "originalFilename": "doc.pdf", "size": 1024,
  "message": "업로드가 허용되었습니다." }

// 거부
{ "result": "REJECTED", "error": "BLOCKED_EXTENSION",
  "message": "차단된 확장자입니다. (.exe)",
  "originalFilename": "virus.exe", "matchedExtension": "exe" }
```

**거부 사유**

| `error` | 의미 |
|---|---|
| `EMPTY` / `EMPTY_FILE` | 파일명 없음 / 빈 파일 |
| `TOO_LONG` | 파일명 길이 초과 |
| `PATH_TRAVERSAL` | 경로 문자 포함 |
| `CONTROL_CHARACTER` | 제어·서식 문자 포함 (RTLO, NUL 등) |
| `BLOCKED_EXTENSION` | 차단 목록의 확장자 |
| `EXECUTABLE_CONTENT` | 확장자와 무관하게 실행 파일로 판별 |
| `PAYLOAD_TOO_LARGE` | 크기 초과 (HTTP 413) |

### 측정용 (개발 편의)

| Method | Path | 설명 |
|---|---|---|
| `GET` | `/api/probe/status` | uptime, `/tmp` 잔존 파일, DB 지연 |
| `POST` | `/api/probe/upload` | 검증 없이 수신 정보만 반환 |

배포 환경의 제약(바디 한도, FS 휘발성, MIME 스푸핑)을 실측하기 위해 만든 엔드포인트입니다.
측정 결과는 CONSIDERATIONS.md 1절에 있습니다.

---

## 프로젝트 구조

```
src/main/java/com/jeonghuny/ext_blocker/
├── validation/                     # 순수 로직 (DB 의존 없음)
│   ├── FileNameNormalizer          # 파일명 → 정제된 이름 + 확장자 조각
│   ├── ExtensionPolicyValidator    # 조각 + 차단목록 → 허용/거부 판정
│   ├── NormalizedFileName          # 정규화 결과
│   ├── PolicyDecision              # 판정 결과
│   └── RejectReason                # 거부 사유 + 사용자 메시지
├── policy/                         # 정책 CRUD
│   ├── BlockedExtension            # 엔티티
│   ├── ExtensionPolicyService      # 입력 정규화, 중복/한도 처리
│   └── ExtensionPolicyController
└── upload/                         # 업로드 검증
    ├── UploadController
    └── FileSignatureInspector      # 매직 넘버 기반 실행 파일 탐지

src/main/resources/
├── static/index.html               # 관리 화면 (빌드 불필요)
└── db/migration/V1__init.sql       # 스키마 + 초기 데이터
```

`validation` 패키지는 **정책과 DB를 모릅니다.** 순수 문자열 처리라
DB 없이 빠르게 테스트되며, 44건의 단위 테스트가 이 패키지를 검증합니다.

---

## 주요 판단

자세한 근거는 [CONSIDERATIONS.md](./CONSIDERATIONS.md) 안에 있습니다.

- **위협 모델 재정의** — 확장자 차단은 방어의 본체가 아니라 한 겹입니다. (0절)
- **파일 미저장** — 컨테이너 FS 휘발성 실측 + 공개 URL에서의 악성파일 호스팅 방지. (2-7절)
- **시그니처 검증의 범위 한정** — ZIP 계열(`docx`/`jar`) 구분 불가라는 한계를 명시하고
  "실행 파일 탐지"로 좁혔습니다. (2-1절)
- **이중 확장자 전량 검사** — 오탐을 인지하고 감수한 선택입니다. (2-2절)
- **클라이언트 + 서버 이중 크기 검증** — 서버 413이 브라우저에 도달하지 않을 수 있음을
  실측한 결과입니다. (1-1절)
