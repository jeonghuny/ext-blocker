package com.jeonghuny.ext_blocker.validation;

import java.util.List;

/**
 * 파일명 정규화 결과.
 *
 * @param original          원본 파일명 (로그/감사용, 절대 저장 경로에 사용 금지)
 * @param sanitized         정제된 파일명 (NFC 정규화, 후행 점·공백 제거)
 * @param extensionSegments 점으로 분리한 확장자 후보 (소문자, 앞→뒤 순서)
 * @param rejectReason      거부 사유. null이면 정상
 */
public record NormalizedFileName(
        String original,
        String sanitized,
        List<String> extensionSegments,
        RejectReason rejectReason
) {
    public boolean isRejected() {
        return rejectReason != null;
    }

    public static NormalizedFileName rejected(String original, RejectReason reason) {
        return new NormalizedFileName(original, null, List.of(), reason);
    }
}