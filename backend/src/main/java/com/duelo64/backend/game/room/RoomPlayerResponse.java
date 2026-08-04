package com.duelo64.backend.game.room;

import java.util.UUID;

import com.duelo64.backend.user.User;

public record RoomPlayerResponse(
        UUID id,
        String email,
        String nickname,
        String avatarUrl) {

    public static RoomPlayerResponse from(User user) {
        if (user == null) {
            return null;
        }

        return new RoomPlayerResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getAvatarUrl());
    }
}
