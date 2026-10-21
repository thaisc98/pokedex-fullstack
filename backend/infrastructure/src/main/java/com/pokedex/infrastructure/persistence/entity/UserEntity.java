package com.pokedex.infrastructure.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;
import org.jspecify.annotations.Nullable;

/** {@code APP_USER} row (architecture.md §2.7). Never leaves infrastructure; never bound to JSON. */
@Entity
@Table(name = "app_user")
public class UserEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "email", nullable = false, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "display_name", nullable = false, length = 60)
    private String displayName;

    @Column(name = "role", nullable = false, length = 16)
    private String role;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /** {@code null} until persisted, which also tells Spring Data the entity is new (persist, not merge). */
    @Version
    @Column(name = "version", nullable = false)
    private @Nullable Long version;

    /** For JPA only. */
    protected UserEntity() {}

    public UserEntity(
            UUID id,
            String email,
            String passwordHash,
            String displayName,
            String role,
            Instant createdAt,
            @Nullable Long version) {
        this.id = id;
        this.email = email;
        this.passwordHash = passwordHash;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = createdAt;
        this.version = version;
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getRole() {
        return role;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public @Nullable Long getVersion() {
        return version;
    }
}
