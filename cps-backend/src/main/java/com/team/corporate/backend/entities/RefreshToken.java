package com.team.corporate.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.jetbrains.annotations.NotNull;

import java.time.Instant;

@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "refresh_tokens_session_idx", columnList = "session_id"))
public class RefreshToken {
    @Id
    @Column(name = "token_hash", length = 64)
    private String tokenHash;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private RefreshSession session;

    @Column(name = "used_at")
    private Instant usedAt;

    protected RefreshToken() {
    }

    public RefreshToken(@NotNull String tokenHash, @NotNull RefreshSession session) {
        this.tokenHash = tokenHash;
        this.session = session;
    }

    @JsonIgnore
    public String getTokenHash() { return tokenHash; }
    public RefreshSession getSession() { return session; }
    public Instant getUsedAt() { return usedAt; }
    public void markUsed(Instant now) { usedAt = now; }
}
