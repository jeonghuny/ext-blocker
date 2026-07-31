package com.jeonghuny.ext_blocker.policy;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BlockedExtensionRepository extends JpaRepository<BlockedExtension, Long> {

    Optional<BlockedExtension> findByName(String name);

    List<BlockedExtension> findByTypeOrderByCreatedAtAsc(ExtensionType type);

    long countByType(ExtensionType type);

    /** 업로드 검증용 — 실제로 차단 중인 이름만 */
    @Query("select e.name from BlockedExtension e where e.blocked = true")
    Set<String> findAllBlockedNames();
}