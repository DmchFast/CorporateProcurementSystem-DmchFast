package com.team.corporate.backend.dto;

public record TokenResponse(
        String accessToken, String refreshToken, String tokenType,
        long expiresIn, long refreshExpiresIn
) {
}
