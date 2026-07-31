package com.jeonghuny.ext_blocker.validation;

import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Set;

/** 탐색용 - 수동 실행. 발견한 케이스는 정식 테스트로 승격시킬 것 */
class BypassExplorationTest {

    private static final Set<String> BLOCKED =
            Set.of("bat", "cmd", "com", "cpl", "exe", "scr", "js");

    @Test
    void 후보_일괄_확인() {
        List<String> candidates = List.of(
                "virus.exe",          // 대조군 - 차단되어야 정상
                "virus\uFF0Eexe",     // 전각 마침표 U+FF0E
                "virus.\u0435xe",     // 키릴 e (호모글리프)
                "virus.exe\u00A0",    // 후행 NBSP
                "virus.exe\u3000",    // 후행 전각 공백
                "virus.txt::$DATA",   // NTFS ADS
                "virus%2eexe",        // URL 인코딩
                "virus.EXE\u200B",    // ZWSP
                "virus.e\u0301xe"     // 결합 문자
                // ← AI가 준 후보를 여기에 추가
        );

        var validator = new ExtensionPolicyValidator();
        for (String c : candidates) {
            var d = validator.validate(c, BLOCKED);
            System.out.printf("%-8s | %-18s | %s%n",
                    d.allowed() ? "통과" : "차단",
                    d.reason() == null ? "-" : d.reason(),
                    escape(c));
        }
    }

    /** 비가시 문자를 코드포인트로 표시 */
    private static String escape(String s) {
        var sb = new StringBuilder();
        s.codePoints().forEach(cp -> {
            if (cp < 0x20 || cp > 0x7E) sb.append(String.format("\\u%04X", cp));
            else sb.append((char) cp);
        });
        return sb.toString();
    }
}