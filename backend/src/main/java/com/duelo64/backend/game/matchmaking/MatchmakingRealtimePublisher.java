package com.duelo64.backend.game.matchmaking;

import java.util.UUID;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
public class MatchmakingRealtimePublisher {
    private final SimpMessagingTemplate messagingTemplate;

    public MatchmakingRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void matchFound(UUID userId, MatchFoundResponse response) {
        messagingTemplate.convertAndSendToUser(
                userId.toString(),
                "/queue/matchmaking",
                response);
    }
}
