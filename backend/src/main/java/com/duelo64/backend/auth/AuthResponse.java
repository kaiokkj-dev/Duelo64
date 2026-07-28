package com.duelo64.backend.auth;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        AuthUserResponse user
) {
}