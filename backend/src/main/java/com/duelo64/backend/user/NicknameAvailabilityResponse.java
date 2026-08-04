package com.duelo64.backend.user;

public record NicknameAvailabilityResponse(
        String nickname,
        boolean available) {
}
