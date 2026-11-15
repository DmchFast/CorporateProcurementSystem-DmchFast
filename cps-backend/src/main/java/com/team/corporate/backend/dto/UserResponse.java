package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.User;
import com.team.corporate.core.entities.UserRole;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String username, UserRole role, Instant createdAt, boolean enabled) {
    public static UserResponse of(User user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getUserRole(), user.getCreatedAt(), user.isEnabled());
    }
}
