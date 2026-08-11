package com.duelo64.backend.shared.config;

import java.util.Locale;
import java.util.UUID;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameRoomRepository;

@Component
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";
    private static final String ROOM_APP_PREFIX = "/app/rooms/";

    private final JwtDecoder jwtDecoder;
    private final GameRoomRepository gameRoomRepository;

    public WebSocketAuthChannelInterceptor(JwtDecoder jwtDecoder, GameRoomRepository gameRoomRepository) {
        this.jwtDecoder = jwtDecoder;
        this.gameRoomRepository = gameRoomRepository;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null) return message;

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())
                || StompCommand.SEND.equals(accessor.getCommand())) {
            authorizeRoomDestination(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String authorization = accessor.getFirstNativeHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new AccessDeniedException("Token WebSocket ausente.");
        }

        Jwt jwt = jwtDecoder.decode(authorization.substring(7));
        accessor.setUser(new JwtAuthenticationToken(jwt));
    }

    private void authorizeRoomDestination(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) return;

        String prefix;
        if (destination.startsWith(ROOM_TOPIC_PREFIX)) {
            prefix = ROOM_TOPIC_PREFIX;
        } else if (destination.startsWith(ROOM_APP_PREFIX)) {
            prefix = ROOM_APP_PREFIX;
        } else {
            return;
        }

        if (accessor.getUser() == null) throw new AccessDeniedException("WebSocket nao autenticado.");

        UUID userId = UUID.fromString(accessor.getUser().getName());
        String roomPath = destination.substring(prefix.length());
        String roomCode = roomPath.split("/", 2)[0].trim().toUpperCase(Locale.ROOT);
        GameRoom room = gameRoomRepository.findByCode(roomCode)
                .orElseThrow(() -> new AccessDeniedException("Sala nao encontrada."));
        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest() != null && room.getGuest().getId().equals(userId);

        if (!isHost && !isGuest) {
            throw new AccessDeniedException("Voce nao participa desta sala.");
        }
    }
}
