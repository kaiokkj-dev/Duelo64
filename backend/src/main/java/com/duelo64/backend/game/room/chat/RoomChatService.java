package com.duelo64.backend.game.room.chat;

import java.time.Clock;
import java.util.Locale;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameRoomRepository;
import com.duelo64.backend.user.User;

@Service
public class RoomChatService {

    public static final int MAX_MESSAGE_LENGTH = 300;

    private final GameRoomRepository gameRoomRepository;
    private final RoomChatRateLimiter rateLimiter;
    private final Clock clock;

    @Autowired
    public RoomChatService(GameRoomRepository gameRoomRepository, RoomChatRateLimiter rateLimiter) {
        this(gameRoomRepository, rateLimiter, Clock.systemUTC());
    }

    RoomChatService(GameRoomRepository gameRoomRepository, RoomChatRateLimiter rateLimiter, Clock clock) {
        this.gameRoomRepository = gameRoomRepository;
        this.rateLimiter = rateLimiter;
        this.clock = clock;
    }

    public RoomChatMessage createMessage(String roomCode, UUID userId, String rawText) {
        GameRoom room = gameRoomRepository.findByCode(normalizeCode(roomCode))
                .orElseThrow(() -> new IllegalArgumentException("Sala nao encontrada."));
        User sender = participant(room, userId);
        String text = normalizeText(rawText);

        if (!rateLimiter.tryAcquire(userId)) {
            throw new IllegalStateException("Muitas mensagens. Aguarde alguns segundos.");
        }

        String nickname = sender.getNickname();
        if (nickname == null || nickname.isBlank()) nickname = "Jogador";
        return new RoomChatMessage(userId, nickname, text, clock.instant());
    }

    private User participant(GameRoom room, UUID userId) {
        if (room.getHost().getId().equals(userId)) return room.getHost();
        if (room.getGuest() != null && room.getGuest().getId().equals(userId)) return room.getGuest();
        throw new AccessDeniedException("Voce nao participa desta sala.");
    }

    private String normalizeText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            throw new IllegalArgumentException("A mensagem nao pode estar vazia.");
        }
        String text = rawText.trim();
        if (text.length() > MAX_MESSAGE_LENGTH) {
            throw new IllegalArgumentException("A mensagem deve ter no maximo 300 caracteres.");
        }
        return text;
    }

    private String normalizeCode(String roomCode) {
        return roomCode == null ? "" : roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
