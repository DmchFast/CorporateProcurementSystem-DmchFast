package com.team.corporate.backend.entities;

import com.team.corporate.core.entities.User;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "refresh_sessions", indexes = {
        @Index(name = "refresh_sessions_user_idx", columnList = "user_id"),
        @Index(name = "refresh_sessions_expiry_idx", columnList = "expires_at")
})
public class RefreshSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private User user;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected RefreshSession() {
    }

    public RefreshSession(@NotNull User user, @NotNull Instant createdAt, @NotNull Instant expiresAt) {
        this.user = user;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    @NotNull
    public UUID getId() { return id; }

    @NotNull
    public User getUser() { return user; }

    @NotNull
    public Instant getExpiresAt() { return expiresAt; }

    @NotNull
    public Instant getRevokedAt() { return revokedAt; }

    public boolean isActive(@NotNull Instant now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(@NotNull Instant now) {
        if (revokedAt == null) revokedAt = now;
    }
}
