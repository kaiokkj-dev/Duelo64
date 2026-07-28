package com.duelo64.backend.auth;

import java.util.UUID;

import com.duelo64.backend.user.User;

public record AuthUserResponse(
        UUID id,
        String email,
        String nickname,
        String avatarUrl
) {

    public static AuthUserResponse from(User user) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl()
        );
    }
}