package com.team.corporate.backend.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.team.corporate.core.entities.User;
import jakarta.persistence.*;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

@Entity
@Table(name = "user_credentials")
public class UserCredentials {
    @Id
    @Column(name = "user_id")
    private UUID userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    protected UserCredentials() {
    }

    public UserCredentials(@NotNull User user, @NotNull String passwordHash) {
        this.user = user;
        this.passwordHash = passwordHash;
    }

    @NotNull
    public UUID getUserId() {
        return userId;
    }

    @NotNull
    public User getUser() {
        return user;
    }

    @NotNull
    @JsonIgnore
    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(@NotNull String passwordHash) {
        this.passwordHash = passwordHash;
    }
}
