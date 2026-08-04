package com.duelo64.backend.game.room;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class RoomRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RoomRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(RoomRealtimeEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomCode(),
                event);
    }
}
