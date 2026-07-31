package com.jeonghuny.ext_blocker.policy;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

@Entity
@Table(name = "blocked_extension")
public class BlockedExtension {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ExtensionType type;

    @Column(name = "is_blocked", nullable = false)
    private boolean blocked;

    @Column(name = "created_at", nullable = false, updatable = false,
            insertable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false, insertable = false)
    private OffsetDateTime updatedAt;

    protected BlockedExtension() { }   // JPA용

    private BlockedExtension(String name, ExtensionType type, boolean blocked) {
        this.name = name;
        this.type = type;
        this.blocked = blocked;
    }

    /** 커스텀 확장자는 추가 행위 자체가 차단 의사이므로 blocked=true */
    public static BlockedExtension custom(String name) {
        return new BlockedExtension(name, ExtensionType.CUSTOM, true);
    }

    public void changeBlocked(boolean blocked) {
        this.blocked = blocked;
    }

    public Long getId()          { return id; }
    public String getName()      { return name; }
    public ExtensionType getType() { return type; }
    public boolean isBlocked()   { return blocked; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
}