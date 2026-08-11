package com.duelo64.backend.game.room;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import jakarta.annotation.PreDestroy;

@Service
public class RoomPresenceService {

    private final GameRoomRepository gameRoomRepository;
    private final RoomRealtimePublisher roomRealtimePublisher;
    private final long disconnectGraceMillis;
    private final ScheduledExecutorService scheduler;
    private final Map<PresenceKey, Set<String>> sessionsByPlayer = new HashMap<>();
    private final Map<String, Set<PresenceKey>> playersBySession = new HashMap<>();
    private final Map<PresenceKey, ScheduledFuture<?>> pendingDisconnects = new HashMap<>();

    @Autowired
    public RoomPresenceService(
            GameRoomRepository gameRoomRepository,
            RoomRealtimePublisher roomRealtimePublisher,
            @Value("${duelo64.presence.disconnect-grace-ms:3000}") long disconnectGraceMillis) {
        this(gameRoomRepository, roomRealtimePublisher, disconnectGraceMillis,
                Executors.newSingleThreadScheduledExecutor(runnable -> {
                    Thread thread = new Thread(runnable, "room-presence");
                    thread.setDaemon(true);
                    return thread;
                }));
    }

    RoomPresenceService(
            GameRoomRepository gameRoomRepository,
            RoomRealtimePublisher roomRealtimePublisher,
            long disconnectGraceMillis,
            ScheduledExecutorService scheduler) {
        this.gameRoomRepository = gameRoomRepository;
        this.roomRealtimePublisher = roomRealtimePublisher;
        this.disconnectGraceMillis = disconnectGraceMillis;
        this.scheduler = scheduler;
    }

    public void connect(String sessionId, UUID userId, String roomCode) {
        GameRoom room = requireParticipant(userId, roomCode);
        PresenceKey key = new PresenceKey(room.getCode(), userId);
        boolean becameOnline;
        List<UUID> connectedUsers;

        synchronized (this) {
            ScheduledFuture<?> pendingDisconnect = pendingDisconnects.remove(key);
            if (pendingDisconnect != null) pendingDisconnect.cancel(false);

            Set<String> sessions = sessionsByPlayer.computeIfAbsent(key, ignored -> new HashSet<>());
            becameOnline = sessions.isEmpty();
            sessions.add(sessionId);
            playersBySession.computeIfAbsent(sessionId, ignored -> new HashSet<>()).add(key);
            connectedUsers = connectedUsers(room.getCode());
        }

        if (becameOnline) {
            roomRealtimePublisher.publish(RoomRealtimeEvent.playerConnected(room, userId));
        }
        roomRealtimePublisher.publish(RoomRealtimeEvent.presenceSnapshot(room, connectedUsers));
    }

    public synchronized void disconnect(String sessionId) {
        Set<PresenceKey> keys = playersBySession.remove(sessionId);
        if (keys == null) return;

        for (PresenceKey key : keys) {
            Set<String> sessions = sessionsByPlayer.get(key);
            if (sessions == null) continue;
            sessions.remove(sessionId);
            if (!sessions.isEmpty() || pendingDisconnects.containsKey(key)) continue;

            ScheduledFuture<?> future = scheduler.schedule(
                    () -> confirmDisconnect(key),
                    disconnectGraceMillis,
                    TimeUnit.MILLISECONDS);
            pendingDisconnects.put(key, future);
        }
    }

    private void confirmDisconnect(PresenceKey key) {
        GameRoom room;
        synchronized (this) {
            Set<String> sessions = sessionsByPlayer.get(key);
            if (sessions != null && !sessions.isEmpty()) return;
            if (pendingDisconnects.remove(key) == null) return;
            sessionsByPlayer.remove(key);
            room = gameRoomRepository.findByCode(key.roomCode()).orElse(null);
        }

        if (room != null) {
            roomRealtimePublisher.publish(RoomRealtimeEvent.playerDisconnected(room, key.userId()));
        }
    }

    void confirmDisconnectNow(String roomCode, UUID userId) {
        confirmDisconnect(new PresenceKey(normalize(roomCode), userId));
    }

    synchronized boolean isOnline(String roomCode, UUID userId) {
        Set<String> sessions = sessionsByPlayer.get(new PresenceKey(normalize(roomCode), userId));
        return sessions != null && !sessions.isEmpty();
    }

    private List<UUID> connectedUsers(String roomCode) {
        List<UUID> users = new ArrayList<>();
        for (Map.Entry<PresenceKey, Set<String>> entry : sessionsByPlayer.entrySet()) {
            if (entry.getKey().roomCode().equals(roomCode) && !entry.getValue().isEmpty()) {
                users.add(entry.getKey().userId());
            }
        }
        return users;
    }

    private GameRoom requireParticipant(UUID userId, String roomCode) {
        GameRoom room = gameRoomRepository.findByCode(normalize(roomCode))
                .orElseThrow(() -> new AccessDeniedException("Sala nao encontrada."));
        boolean isHost = room.getHost().getId().equals(userId);
        boolean isGuest = room.getGuest() != null && room.getGuest().getId().equals(userId);
        if (!isHost && !isGuest) throw new AccessDeniedException("Voce nao participa desta sala.");
        return room;
    }

    private String normalize(String roomCode) {
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }

    @PreDestroy
    void shutdown() {
        scheduler.shutdownNow();
    }

    private record PresenceKey(String roomCode, UUID userId) {
    }
}
