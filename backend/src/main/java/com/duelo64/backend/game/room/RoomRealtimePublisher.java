package com.duelo64.backend.game.room;

import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
public class RoomRealtimePublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public RoomRealtimePublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publish(RoomRealtimeEvent event) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(event);
                }
            });
            return;
        }

        send(event);
    }

    private void send(RoomRealtimeEvent event) {
        messagingTemplate.convertAndSend(
                "/topic/rooms/" + event.roomCode(),
                event);
    }
}
