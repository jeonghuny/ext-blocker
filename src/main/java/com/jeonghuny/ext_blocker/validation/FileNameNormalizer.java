package com.jeonghuny.ext_blocker.validation;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

public class FileNameNormalizer {

    public static final int MAX_LENGTH = 255;

    /** Cc=제어문자(NUL, TAB...), Cf=서식문자(RTLO, ZWJ...) */
    private static final Pattern CONTROL_OR_FORMAT = Pattern.compile("[\\p{Cc}\\p{Cf}]");

    public NormalizedFileName normalize(String raw) {
        // 1. 빈 값
        if (raw == null || raw.isBlank()) {
            return NormalizedFileName.rejected(raw, RejectReason.EMPTY);
        }

        // 2. 제어·서식 문자 (RTLO 위장, NUL 절단)
        if (CONTROL_OR_FORMAT.matcher(raw).find()) {
            return NormalizedFileName.rejected(raw, RejectReason.CONTROL_CHARACTER);
        }

        // 3. 경로 조작 — 반드시 후행 제거보다 먼저.
        //    ".."를 먼저 자르면 ""가 되어 EMPTY로 잘못 분류됨
        if (raw.indexOf('/') >= 0 || raw.indexOf('\\') >= 0 || "..".equals(raw)) {
            return NormalizedFileName.rejected(raw, RejectReason.PATH_TRAVERSAL);
        }

        // 4. NFC 정규화 — 길이 검사보다 먼저.
        //    NFD는 자모가 분리돼 문자 수가 늘어나므로 정상 파일이 TOO_LONG으로 튕김
        String nfc = Normalizer.normalize(raw, Normalizer.Form.NFC);

        // 5. 후행 점·공백 반복 제거 (Windows가 파일 생성 시 제거하는 문자)
        String sanitized = stripTrailingDotsAndSpaces(nfc);

        // 6. 제거 후 빈 값이 될 수 있음 (".", "...")
        if (sanitized.isEmpty()) {
            return NormalizedFileName.rejected(raw, RejectReason.EMPTY);
        }

        // 7. 길이
        if (sanitized.length() > MAX_LENGTH) {
            return NormalizedFileName.rejected(raw, RejectReason.TOO_LONG);
        }

        return new NormalizedFileName(raw, sanitized, extractSegments(sanitized), null);
    }

    private String stripTrailingDotsAndSpaces(String s) {
        int end = s.length();
        while (end > 0) {
            char c = s.charAt(end - 1);
            if (c != '.' && c != ' ') break;
            end--;
        }
        return s.substring(0, end);
    }

    /**
     * 점으로 분리 후 첫 조각을 버린다.
     * 이 규칙 하나로 두 함정이 동시에 해결된다:
     *   "readme" → [readme] → []       (split.pop()은 readme를 확장자로 오인)
     *   ".env"   → ["", env] → [env]   (extname()은 빈 문자열 반환)
     */
    private List<String> extractSegments(String sanitized) {
        String[] parts = sanitized.split("\\.", -1);
        return Arrays.stream(parts)
                .skip(1)
                .filter(p -> !p.isEmpty())
                .map(p -> p.toLowerCase(Locale.ROOT))
                .toList();
    }
}