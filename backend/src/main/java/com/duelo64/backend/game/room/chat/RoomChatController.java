package com.duelo64.backend.game.room.chat;

import java.security.Principal;
import java.util.UUID;

import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

@Controller
public class RoomChatController {

    private final RoomChatService roomChatService;
    private final SimpMessagingTemplate messagingTemplate;

    public RoomChatController(RoomChatService roomChatService, SimpMessagingTemplate messagingTemplate) {
        this.roomChatService = roomChatService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{roomCode}/chat")
    public void send(
            @DestinationVariable String roomCode,
            @Payload RoomChatMessageRequest request,
            Principal principal) {

        if (principal == null) throw new IllegalStateException("WebSocket nao autenticado.");
        RoomChatMessage message = roomChatService.createMessage(
                roomCode,
                UUID.fromString(principal.getName()),
                request == null ? null : request.text());
        messagingTemplate.convertAndSend("/topic/rooms/" + roomCode.toUpperCase() + "/chat", message);
    }

    @MessageExceptionHandler({ IllegalArgumentException.class, IllegalStateException.class })
    @SendToUser("/queue/chat-errors")
    public RoomChatError handleChatError(RuntimeException exception) {
        return new RoomChatError(exception.getMessage());
    }
}
