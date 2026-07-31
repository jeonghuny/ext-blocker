package com.jeonghuny.ext_blocker.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("파일명 정규화")
class FileNameNormalizerTest {

    private FileNameNormalizer normalizer;

    @BeforeEach
    void setUp() {
        normalizer = new FileNameNormalizer();
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("확장자 추출")
    class ExtensionExtraction {

        @Test
        @DisplayName("일반 파일명은 마지막 조각을 확장자로 인식한다")
        void 일반_파일명() {
            var r = normalizer.normalize("normal.txt");
            assertThat(r.isRejected()).isFalse();
            assertThat(r.extensionSegments()).containsExactly("txt");
        }

        @Test
        @DisplayName("[함정] 확장자 없는 파일은 확장자 목록이 비어야 한다 " +
                "— split('.').pop() 방식은 파일명 전체를 확장자로 오인함")
        void 확장자_없음() {
            var r = normalizer.normalize("readme");
            assertThat(r.isRejected()).isFalse();
            assertThat(r.extensionSegments()).isEmpty();
        }

        @Test
        @DisplayName("[함정] 점으로 시작하는 파일은 확장자가 있는 것으로 인식해야 한다 " +
                "— path.extname('.env')는 빈 문자열을 반환해 검사를 통째로 건너뜀")
        void dotfile() {
            var r = normalizer.normalize(".env");
            assertThat(r.isRejected()).isFalse();
            assertThat(r.extensionSegments()).containsExactly("env");
        }

        @Test
        @DisplayName("이중 확장자는 모든 조각을 추출한다")
        void 이중_확장자() {
            var r = normalizer.normalize("file.exe.txt");
            assertThat(r.extensionSegments()).containsExactly("exe", "txt");
        }

        @Test
        @DisplayName("tar.gz는 두 조각으로 분리한다")
        void 압축_이중_확장자() {
            var r = normalizer.normalize("archive.tar.gz");
            assertThat(r.extensionSegments()).containsExactly("tar", "gz");
        }

        @Test
        @DisplayName("확장자는 소문자로 정규화한다")
        void 대소문자_정규화() {
            var r = normalizer.normalize("FILE.EXE");
            assertThat(r.extensionSegments()).containsExactly("exe");
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("보이지 않는 문자 처리")
    class InvisibleCharacters {

        @Test
        @DisplayName("[실측] 후행 점은 제거한다 — Windows가 파일 생성 시 끝 점을 떼어내므로 " +
                "'file.exe.'는 실제로 file.exe가 됨")
        void 후행_점() {
            var r = normalizer.normalize("file.exe.");
            assertThat(r.sanitized()).isEqualTo("file.exe");
            assertThat(r.extensionSegments()).containsExactly("exe");
        }

        @Test
        @DisplayName("[실측] 후행 공백은 제거한다 — 서버가 원본 그대로 수신함을 curl로 확인")
        void 후행_공백() {
            var r = normalizer.normalize("file.exe ");
            assertThat(r.sanitized()).isEqualTo("file.exe");
            assertThat(r.extensionSegments()).containsExactly("exe");
        }

        @Test
        @DisplayName("점과 공백이 섞인 후행 문자도 반복 제거한다")
        void 후행_혼합() {
            var r = normalizer.normalize("file.exe. . .");
            assertThat(r.sanitized()).isEqualTo("file.exe");
            assertThat(r.extensionSegments()).containsExactly("exe");
        }

        @Test
        @DisplayName("RTLO(U+202E) 등 제어·서식 문자가 있으면 거부한다 " +
                "— 'photo\\u202Egnp.exe'는 화면에 photo_exe.png로 보임")
        void RTLO_위장() {
            var r = normalizer.normalize("photo\u202Egnp.exe");
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.CONTROL_CHARACTER);
        }

        @Test
        @DisplayName("NUL 문자가 포함되면 거부한다")
        void NUL_문자() {
            var r = normalizer.normalize("file.txt\u0000.exe");
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.CONTROL_CHARACTER);
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("유니코드 정규화")
    class UnicodeNormalization {

        @Test
        @DisplayName("[실측] 서버는 NFC/NFD를 정규화하지 않으므로 애플리케이션에서 NFC로 통일한다")
        void NFD_입력을_NFC로() {
            String nfd = Normalizer.normalize("한글파일.txt", Normalizer.Form.NFD);

            // 전제 확인: NFD와 NFC는 실제로 다른 문자열이다
            assertThat(nfd).isNotEqualTo("한글파일.txt");

            var r = normalizer.normalize(nfd);

            assertThat(r.sanitized()).isEqualTo("한글파일.txt");
            assertThat(Normalizer.isNormalized(r.sanitized(), Normalizer.Form.NFC)).isTrue();
        }

        @Test
        @DisplayName("NFC로 들어온 파일명은 그대로 유지된다")
        void NFC_입력() {
            var r = normalizer.normalize("한글파일.txt");
            assertThat(r.sanitized()).isEqualTo("한글파일.txt");
            assertThat(r.extensionSegments()).containsExactly("txt");
        }

        @Test
        @DisplayName("NFC/NFD로 입력된 같은 이름은 정규화 후 동일해진다")
        void 정규화_후_동일성() {
            String nfc = "한글파일.txt";
            String nfd = Normalizer.normalize(nfc, Normalizer.Form.NFD);

            assertThat(normalizer.normalize(nfd).sanitized())
                    .isEqualTo(normalizer.normalize(nfc).sanitized());
        }
    }

    // ------------------------------------------------------------
    @Nested
    @DisplayName("거부 케이스")
    class Rejection {

        @ParameterizedTest
        @DisplayName("[실측] 경로 문자가 포함되면 거부한다 — 프레임워크가 정제하지 않음을 curl로 확인")
        @ValueSource(strings = {
                "../../etc/passwd",
                "..\\..\\windows\\system32\\cmd.exe",
                "/etc/passwd",
                "dir/file.txt",
                ".."
        })
        void 경로_조작(String raw) {
            var r = normalizer.normalize(raw);
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.PATH_TRAVERSAL);
        }

        @ParameterizedTest
        @DisplayName("비어 있거나 공백뿐인 파일명은 거부한다")
        @ValueSource(strings = {"", "   ", ".", "..."})
        void 빈_파일명(String raw) {
            var r = normalizer.normalize(raw);
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.EMPTY);
        }

        @Test
        @DisplayName("null 파일명은 거부한다")
        void null_파일명() {
            var r = normalizer.normalize(null);
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.EMPTY);
        }

        @Test
        @DisplayName("[실측] 최대 길이를 초과하면 거부한다 — 304자가 잘림 없이 도착함을 확인")
        void 너무_긴_파일명() {
            String longName = "long" + "a".repeat(300) + ".txt";
            assertThat(longName.length()).isGreaterThan(FileNameNormalizer.MAX_LENGTH);

            var r = normalizer.normalize(longName);
            assertThat(r.isRejected()).isTrue();
            assertThat(r.rejectReason()).isEqualTo(RejectReason.TOO_LONG);
        }

        @Test
        @DisplayName("경계값: 최대 길이와 같으면 통과한다")
        void 경계값_길이() {
            String name = "a".repeat(FileNameNormalizer.MAX_LENGTH - 4) + ".txt";
            assertThat(name).hasSize(FileNameNormalizer.MAX_LENGTH);

            var r = normalizer.normalize(name);
            assertThat(r.isRejected()).isFalse();
        }
    }

    // ------------------------------------------------------------
    @Test
    @DisplayName("원본 파일명은 로그/감사를 위해 그대로 보존한다")
    void 원본_보존() {
        var r = normalizer.normalize("FILE.EXE. ");
        assertThat(r.original()).isEqualTo("FILE.EXE. ");
        assertThat(r.sanitized()).isEqualTo("FILE.EXE");
    }

    @Test
    @DisplayName("[적대적 테스트 발견] NBSP 등 유니코드 공백류도 후행 제거한다")
    void 유니코드_공백_후행() {
        var r = normalizer.normalize("file.exe\u00A0");
        assertThat(r.sanitized()).isEqualTo("file.exe");
        assertThat(r.extensionSegments()).containsExactly("exe");
    }
}