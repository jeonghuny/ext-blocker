package com.jeonghuny.ext_blocker.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("확장자 차단 정책 검증")
class ExtensionPolicyValidatorTest {

    /** V1__init.sql의 고정 확장자 7종이 모두 체크된 상태를 가정 */
    private static final Set<String> BLOCKED =
            Set.of("bat", "cmd", "com", "cpl", "exe", "scr", "js");

    private ExtensionPolicyValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ExtensionPolicyValidator();
    }

    @ParameterizedTest
    @DisplayName("차단 목록에 있는 확장자는 거부하고, 어떤 확장자가 걸렸는지 알려준다")
    @CsvSource({
            "virus.exe,      exe",
            "script.js,      js",
            "installer.bat,  bat",
            "VIRUS.EXE,      exe",   // 대문자
            "virus.exe.,     exe",   // 후행 점
            "'virus.exe ',   exe",   // 후행 공백 — 작은따옴표로 보존
            "virus.exe.txt,  exe"    // 이중 확장자, 앞 조각이 먼저 걸림
    })
    void 차단_대상(String fileName, String expectedMatch) {
        var d = validator.validate(fileName, BLOCKED);

        assertThat(d.allowed()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectReason.BLOCKED_EXTENSION);
        assertThat(d.matchedExtension()).isEqualTo(expectedMatch);
    }

    @Test
    @DisplayName("차단 사유에는 어떤 확장자가 걸렸는지 담긴다 — 사용자에게 이유를 알려야 함")
    void 차단_사유_명시() {
        var d = validator.validate("report.js", BLOCKED);
        assertThat(d.matchedExtension()).isEqualTo("js");
    }

    @ParameterizedTest
    @DisplayName("차단 목록에 없는 확장자는 허용한다")
    @ValueSource(strings = {
            "document.pdf",
            "photo.jpg",
            "archive.tar.gz",
            "readme",           // 확장자 없음 — 허용 정책
            "한글파일.txt"
    })
    void 허용_대상(String fileName) {
        var d = validator.validate(fileName, BLOCKED);
        assertThat(d.allowed()).isTrue();
    }

    @Test
    @DisplayName("[정책] .env는 차단 목록에 env가 있을 때만 거부된다 " +
            "— 확장자 추출이 되는지가 핵심")
    void dotfile_정책() {
        assertThat(validator.validate(".env", BLOCKED).allowed()).isTrue();
        assertThat(validator.validate(".env", Set.of("env")).allowed()).isFalse();
    }

    @Test
    @DisplayName("차단 목록이 비어 있으면 모두 허용한다 — 기본값이 전부 unCheck인 상태")
    void 빈_차단목록() {
        assertThat(validator.validate("virus.exe", Set.of()).allowed()).isTrue();
    }

    @Test
    @DisplayName("파일명 자체가 부적합하면 확장자 검사 이전에 거부한다")
    void 파일명_거부_우선() {
        var d = validator.validate("../../etc/passwd", BLOCKED);
        assertThat(d.allowed()).isFalse();
        assertThat(d.reason()).isEqualTo(RejectReason.PATH_TRAVERSAL);
    }

    @Test
    @DisplayName("차단 목록에 대문자가 섞여 있어도 정상 동작한다 — 목록 쪽도 정규화 필요")
    void 차단목록_대소문자() {
        var d = validator.validate("virus.exe", Set.of("EXE"));
        assertThat(d.allowed()).isFalse();
    }
}