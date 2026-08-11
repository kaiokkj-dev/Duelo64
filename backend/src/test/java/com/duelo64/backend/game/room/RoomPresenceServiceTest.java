package com.duelo64.backend.game.room;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.duelo64.backend.user.User;

class RoomPresenceServiceTest {

    private UUID hostId;
    private UUID guestId;
    private GameRoom room;
    private RoomRealtimePublisher publisher;
    private RoomPresenceService presenceService;

    @BeforeEach
    void setUp() {
        hostId = UUID.randomUUID();
        guestId = UUID.randomUUID();
        User host = user(hostId);
        User guest = user(guestId);
        room = GameRoom.privateCheckers("ABC123", 10, host);
        room.join(guest);
        GameRoomRepository repository = mock(GameRoomRepository.class);
        when(repository.findByCode("ABC123")).thenReturn(Optional.of(room));
        publisher = mock(RoomRealtimePublisher.class);
        presenceService = new RoomPresenceService(repository, publisher, 60_000);
    }

    @AfterEach
    void tearDown() {
        presenceService.shutdown();
    }

    @Test
    void playerConnectionShouldBecomeOnlineAndPublishPresence() {
        presenceService.connect("session-1", hostId, "ABC123");

        assertThat(presenceService.isOnline("ABC123", hostId)).isTrue();
        assertThat(publishedEvents()).anyMatch(event ->
                event.type().equals("PLAYER_CONNECTED") && hostId.equals(event.userId()));
        assertThat(publishedEvents()).anyMatch(event ->
                event.type().equals("PRESENCE_SNAPSHOT") && event.connectedUserIds().contains(hostId));
    }

    @Test
    void lastConnectionClosingShouldBecomeOfflineAfterConfirmation() {
        presenceService.connect("session-1", guestId, "ABC123");

        presenceService.disconnect("session-1");
        presenceService.confirmDisconnectNow("ABC123", guestId);

        assertThat(presenceService.isOnline("ABC123", guestId)).isFalse();
        assertThat(publishedEvents()).anyMatch(event ->
                event.type().equals("PLAYER_DISCONNECTED") && guestId.equals(event.userId()));
        assertThat(room.getStatus()).isEqualTo(RoomStatus.IN_PROGRESS);
    }

    @Test
    void quickReconnectShouldCancelPendingDisconnect() {
        presenceService.connect("session-1", guestId, "ABC123");
        presenceService.disconnect("session-1");

        presenceService.connect("session-2", guestId, "ABC123");
        presenceService.confirmDisconnectNow("ABC123", guestId);

        assertThat(presenceService.isOnline("ABC123", guestId)).isTrue();
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event.type().equals("PLAYER_DISCONNECTED")));
    }

    @Test
    void closingOneOfTwoConnectionsShouldKeepPlayerOnline() {
        presenceService.connect("session-1", guestId, "ABC123");
        presenceService.connect("session-2", guestId, "ABC123");

        presenceService.disconnect("session-1");
        presenceService.confirmDisconnectNow("ABC123", guestId);

        assertThat(presenceService.isOnline("ABC123", guestId)).isTrue();
        verify(publisher, never()).publish(org.mockito.ArgumentMatchers.argThat(event ->
                event.type().equals("PLAYER_DISCONNECTED")));
    }

    private java.util.List<RoomRealtimeEvent> publishedEvents() {
        ArgumentCaptor<RoomRealtimeEvent> captor = ArgumentCaptor.forClass(RoomRealtimeEvent.class);
        verify(publisher, atLeastOnce()).publish(captor.capture());
        return captor.getAllValues();
    }

    private User user(UUID id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }
}
