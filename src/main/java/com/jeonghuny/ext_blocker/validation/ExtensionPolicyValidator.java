package com.jeonghuny.ext_blocker.validation;

import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ExtensionPolicyValidator {

    private final FileNameNormalizer normalizer;

    public ExtensionPolicyValidator() {
        this(new FileNameNormalizer());
    }

    public ExtensionPolicyValidator(FileNameNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public PolicyDecision validate(String rawFileName, Set<String> blockedExtensions) {
        NormalizedFileName name = normalizer.normalize(rawFileName);

        // 파일명 자체가 부적합하면 확장자 검사 이전에 거부
        if (name.isRejected()) {
            return PolicyDecision.deny(name.rejectReason(), null);
        }

        Set<String> blocked = normalizeBlockList(blockedExtensions);

        // 마지막 조각만이 아니라 모든 조각 대조 (이중 확장자 대응)
        for (String segment : name.extensionSegments()) {
            if (blocked.contains(segment)) {
                return PolicyDecision.deny(RejectReason.BLOCKED_EXTENSION, segment);
            }
        }
        return PolicyDecision.allow();
    }

    /** 차단 목록도 정규화한다 — DB에 대문자가 섞여 들어갈 수 있음 */
    private Set<String> normalizeBlockList(Set<String> raw) {
        if (raw == null || raw.isEmpty()) return Set.of();
        return raw.stream()
                .filter(Objects::nonNull)
                .map(e -> e.trim().toLowerCase(Locale.ROOT))
                .filter(e -> !e.isEmpty())
                .collect(Collectors.toSet());
    }
}