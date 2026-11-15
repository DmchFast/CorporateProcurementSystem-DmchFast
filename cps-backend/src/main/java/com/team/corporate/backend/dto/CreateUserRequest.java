package com.team.corporate.backend.dto;

import com.team.corporate.core.entities.UserRole;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank @Size(max = 255) String username,
        @NotBlank @Size(min = 12, max = 72) String password,
        @NotNull UserRole role
) {
}
