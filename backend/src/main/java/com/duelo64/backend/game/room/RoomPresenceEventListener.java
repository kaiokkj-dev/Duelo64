package com.duelo64.backend.game.room;

import java.util.UUID;

import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;
import org.springframework.web.socket.messaging.SessionSubscribeEvent;

@Component
public class RoomPresenceEventListener {

    private static final String ROOM_TOPIC_PREFIX = "/topic/rooms/";

    private final RoomPresenceService roomPresenceService;

    public RoomPresenceEventListener(RoomPresenceService roomPresenceService) {
        this.roomPresenceService = roomPresenceService;
    }

    @EventListener
    public void onSubscribe(SessionSubscribeEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String destination = accessor.getDestination();
        if (destination == null || !destination.startsWith(ROOM_TOPIC_PREFIX) || accessor.getUser() == null) return;

        String roomCode = destination.substring(ROOM_TOPIC_PREFIX.length());
        if (roomCode.contains("/")) return;
        UUID userId = UUID.fromString(accessor.getUser().getName());
        roomPresenceService.connect(accessor.getSessionId(), userId, roomCode);
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        roomPresenceService.disconnect(event.getSessionId());
    }
}
