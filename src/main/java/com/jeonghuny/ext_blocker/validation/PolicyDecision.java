package com.jeonghuny.ext_blocker.validation;

/**
 * @param allowed           허용 여부
 * @param reason            거부 사유 (allowed=true면 null)
 * @param matchedExtension  차단 목록과 일치한 확장자 (없으면 null)
 */
public record PolicyDecision(boolean allowed, RejectReason reason, String matchedExtension) {

    public static PolicyDecision allow() {
        return new PolicyDecision(true, null, null);
    }

    public static PolicyDecision deny(RejectReason reason, String matched) {
        return new PolicyDecision(false, reason, matched);
    }
}