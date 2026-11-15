package com.team.corporate.backend.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull Boolean enabled) {
}
