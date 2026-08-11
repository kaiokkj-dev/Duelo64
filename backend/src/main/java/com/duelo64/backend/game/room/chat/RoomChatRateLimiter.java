package com.duelo64.backend.game.room.chat;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RoomChatRateLimiter {

    static final int MAX_MESSAGES = 6;
    static final Duration WINDOW = Duration.ofSeconds(10);

    private final Map<UUID, Deque<Instant>> messagesByUser = new ConcurrentHashMap<>();
    private final Clock clock;

    @Autowired
    public RoomChatRateLimiter() {
        this(Clock.systemUTC());
    }

    RoomChatRateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(UUID userId) {
        Deque<Instant> messages = messagesByUser.computeIfAbsent(userId, ignored -> new ArrayDeque<>());
        Instant now = clock.instant();
        Instant cutoff = now.minus(WINDOW);

        synchronized (messages) {
            while (!messages.isEmpty() && !messages.peekFirst().isAfter(cutoff)) {
                messages.removeFirst();
            }
            if (messages.size() >= MAX_MESSAGES) return false;
            messages.addLast(now);
            return true;
        }
    }
}
