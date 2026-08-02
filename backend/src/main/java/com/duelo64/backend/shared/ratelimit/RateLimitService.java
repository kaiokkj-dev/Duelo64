package com.duelo64.backend.shared.ratelimit;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class RateLimitService {

    private static final long STALE_ENTRY_SECONDS = Duration.ofHours(2).toSeconds();

    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Long> cooldowns = new ConcurrentHashMap<>();
    private final Clock clock;

    public RateLimitService() {
        this(Clock.systemUTC());
    }

    RateLimitService(Clock clock) {
        this.clock = clock;
    }

    public void check(String key, int maximumRequests, Duration window) {
        if (maximumRequests < 1 || window.isZero() || window.isNegative()) {
            throw new IllegalArgumentException("Configuração de rate limit inválida.");
        }

        long now = clock.instant().getEpochSecond();
        long windowSeconds = window.toSeconds();
        long currentWindowStart = now - Math.floorMod(now, windowSeconds);
        AtomicLong retryAfter = new AtomicLong(0);

        counters.compute(key, (ignored, current) -> {
            if (current == null || current.windowStart() != currentWindowStart) {
                return new WindowCounter(currentWindowStart, 1, now);
            }

            if (current.requests() >= maximumRequests) {
                retryAfter.set(Math.max(1, currentWindowStart + windowSeconds - now));
                return new WindowCounter(current.windowStart(), current.requests(), now);
            }

            return new WindowCounter(current.windowStart(), current.requests() + 1, now);
        });

        if (retryAfter.get() > 0) {
            throw new RateLimitException(retryAfter.get());
        }
    }

    public void checkCooldown(String key, Duration cooldown) {
        if (cooldown.isZero() || cooldown.isNegative()) {
            throw new IllegalArgumentException("Configuração de cooldown inválida.");
        }

        long now = clock.instant().getEpochSecond();
        long cooldownSeconds = cooldown.toSeconds();
        AtomicLong retryAfter = new AtomicLong(0);

        cooldowns.compute(key, (ignored, lastAcceptedAt) -> {
            if (lastAcceptedAt != null && now - lastAcceptedAt < cooldownSeconds) {
                retryAfter.set(cooldownSeconds - (now - lastAcceptedAt));
                return lastAcceptedAt;
            }

            return now;
        });

        if (retryAfter.get() > 0) {
            throw new RateLimitException(retryAfter.get());
        }
    }

    @Scheduled(fixedDelayString = "${RATE_LIMIT_CLEANUP_MILLISECONDS:600000}")
    public void removeStaleEntries() {
        long staleBefore = clock.instant().getEpochSecond() - STALE_ENTRY_SECONDS;
        counters.entrySet().removeIf(entry -> entry.getValue().lastAccess() < staleBefore);
        cooldowns.entrySet().removeIf(entry -> entry.getValue() < staleBefore);
    }

    private record WindowCounter(
            long windowStart,
            int requests,
            long lastAccess) {
    }
}
