package com.duelo64.backend.game.room.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import com.duelo64.backend.game.room.GameRoom;
import com.duelo64.backend.game.room.GameRoomRepository;
import com.duelo64.backend.user.User;

class RoomChatServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-10T18:00:00Z");

    private final GameRoomRepository repository = mock(GameRoomRepository.class);
    private final RoomChatRateLimiter rateLimiter = mock(RoomChatRateLimiter.class);
    private RoomChatService service;
    private GameRoom room;
    private User host;
    private User guest;

    @BeforeEach
    void setUp() {
        host = user("host@duelo64.com", "kaio");
        guest = user("guest@duelo64.com", "rival");
        room = GameRoom.privateCheckers("ABC123", 10, host);
        room.join(guest);
        when(repository.findByCode("ABC123")).thenReturn(Optional.of(room));
        when(rateLimiter.tryAcquire(host.getId())).thenReturn(true);
        when(rateLimiter.tryAcquire(guest.getId())).thenReturn(true);
        service = new RoomChatService(repository, rateLimiter, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void hostCanSendTrimmedMessageWithServerMetadata() {
        RoomChatMessage message = service.createMessage("abc123", host.getId(), "  Boa jogada!  ");

        assertThat(message.userId()).isEqualTo(host.getId());
        assertThat(message.nickname()).isEqualTo("kaio");
        assertThat(message.text()).isEqualTo("Boa jogada!");
        assertThat(message.timestamp()).isEqualTo(NOW);
    }

    @Test
    void guestCanSendMessage() {
        assertThat(service.createMessage("ABC123", guest.getId(), "Valeu").nickname()).isEqualTo("rival");
    }

    @Test
    void emptyMessageIsRejected() {
        assertThatThrownBy(() -> service.createMessage("ABC123", host.getId(), "   "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void oversizedMessageIsRejected() {
        assertThatThrownBy(() -> service.createMessage("ABC123", host.getId(), "a".repeat(301)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void externalUserCannotSend() {
        assertThatThrownBy(() -> service.createMessage("ABC123", UUID.randomUUID(), "Oi"))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void rateLimitRejectsExcessiveMessages() {
        when(rateLimiter.tryAcquire(host.getId())).thenReturn(false);

        assertThatThrownBy(() -> service.createMessage("ABC123", host.getId(), "spam"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Muitas mensagens");
    }

    private User user(String email, String nickname) {
        User user = new User(email);
        ReflectionTestUtils.setField(user, "id", UUID.randomUUID());
        user.updateNickname(nickname);
        return user;
    }
}
