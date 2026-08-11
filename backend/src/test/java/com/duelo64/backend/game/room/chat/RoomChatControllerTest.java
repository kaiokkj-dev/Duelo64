package com.duelo64.backend.game.room.chat;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.security.Principal;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class RoomChatControllerTest {

    @Test
    void publishesOfficialMessageToRoomChatTopic() {
        RoomChatService service = mock(RoomChatService.class);
        SimpMessagingTemplate messagingTemplate = mock(SimpMessagingTemplate.class);
        RoomChatController controller = new RoomChatController(service, messagingTemplate);
        UUID userId = UUID.randomUUID();
        Principal principal = () -> userId.toString();
        RoomChatMessage officialMessage = new RoomChatMessage(
                userId, "kaio", "Boa jogada!", Instant.parse("2026-08-10T18:00:00Z"));
        when(service.createMessage("abc123", userId, "Boa jogada!")).thenReturn(officialMessage);

        controller.send("abc123", new RoomChatMessageRequest("Boa jogada!"), principal);

        verify(messagingTemplate).convertAndSend("/topic/rooms/ABC123/chat", officialMessage);
    }
}
