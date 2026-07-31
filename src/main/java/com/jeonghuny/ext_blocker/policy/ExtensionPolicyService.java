package com.jeonghuny.ext_blocker.policy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.dao.DataIntegrityViolationException;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@Transactional(readOnly = true)
public class ExtensionPolicyService {

    public static final int MAX_CUSTOM = 200;
    public static final int MAX_NAME_LENGTH = 20;

    /** V1__init.sql의 CHECK 제약과 동일한 규칙 */
    private static final Pattern VALID_NAME = Pattern.compile("^[a-z0-9]{1,20}$");

    private final BlockedExtensionRepository repository;

    public ExtensionPolicyService(BlockedExtensionRepository repository) {
        this.repository = repository;
    }

    public List<BlockedExtension> findFixed() {
        return repository.findByTypeOrderByCreatedAtAsc(ExtensionType.FIXED);
    }

    public List<BlockedExtension> findCustom() {
        return repository.findByTypeOrderByCreatedAtAsc(ExtensionType.CUSTOM);
    }

    public long countCustom() {
        return repository.countByType(ExtensionType.CUSTOM);
    }

    public Set<String> findBlockedNames() {
        return repository.findAllBlockedNames();
    }

    @Transactional
    public void toggleFixed(String name, boolean blocked) {
        BlockedExtension e = repository.findByName(normalize(name))
                .orElseThrow(() -> new PolicyException(PolicyError.NOT_FOUND, name));

        if (e.getType() != ExtensionType.FIXED) {
            throw new PolicyException(PolicyError.NOT_FIXED, name);
        }
        e.changeBlocked(blocked);
    }

    @Transactional
    public BlockedExtension addCustom(String rawInput) {
        String name = normalize(rawInput);

        if (!VALID_NAME.matcher(name).matches()) {
            throw new PolicyException(PolicyError.INVALID_FORMAT, rawInput);
        }

        // 고정/커스텀 교차 중복까지 한 번에 판정
        repository.findByName(name).ifPresent(existing -> {
            throw new PolicyException(
                    existing.getType() == ExtensionType.FIXED
                            ? PolicyError.ALREADY_FIXED
                            : PolicyError.DUPLICATE,
                    name);
        });

        if (countCustom() >= MAX_CUSTOM) {
            throw new PolicyException(PolicyError.LIMIT_EXCEEDED, name);
        }

        try {
            return repository.saveAndFlush(BlockedExtension.custom(name));
        } catch (DataIntegrityViolationException ex) {
            // 동시 요청으로 위 검사를 통과한 경우 — DB UNIQUE가 최종 방어선
            throw new PolicyException(PolicyError.DUPLICATE, name);
        }
    }

    @Transactional
    public void deleteCustom(String rawName) {
        String name = normalize(rawName);
        BlockedExtension e = repository.findByName(name)
                .orElseThrow(() -> new PolicyException(PolicyError.NOT_FOUND, name));

        if (e.getType() == ExtensionType.FIXED) {
            throw new PolicyException(PolicyError.CANNOT_DELETE_FIXED, name);
        }
        repository.delete(e);
    }

    /**
     * 입력 정규화. 검사보다 먼저 수행한다.
     * 사용자는 ".exe"처럼 점을 붙여 입력하는 것이 자연스러우므로
     * 이를 거부하지 않고 흡수한다.
     */
    private String normalize(String raw) {
        if (raw == null) return "";
        String s = Normalizer.normalize(raw.trim(), Normalizer.Form.NFC);
        while (s.startsWith(".")) s = s.substring(1);
        return s.trim().toLowerCase(Locale.ROOT);
    }
}