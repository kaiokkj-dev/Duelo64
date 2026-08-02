package com.duelo64.backend.shared.ratelimit;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

class RateLimitServiceTest {

    @Test
    void shouldBlockRequestsAfterWindowLimit() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-02T18:00:00Z"));
        RateLimitService service = new RateLimitService(clock);

        assertDoesNotThrow(() -> service.check("auth:test", 2, Duration.ofMinutes(1)));
        assertDoesNotThrow(() -> service.check("auth:test", 2, Duration.ofMinutes(1)));

        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> service.check("auth:test", 2, Duration.ofMinutes(1)));

        assertEquals(60, exception.getRetryAfterSeconds());
    }

    @Test
    void shouldReleaseLimitInNextWindow() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-02T18:00:00Z"));
        RateLimitService service = new RateLimitService(clock);

        service.check("auth:test", 1, Duration.ofMinutes(1));
        clock.advance(Duration.ofSeconds(60));

        assertDoesNotThrow(() -> service.check("auth:test", 1, Duration.ofMinutes(1)));
    }

    @Test
    void shouldEnforceRealCooldownAcrossWindowBoundaries() {
        MutableClock clock = new MutableClock(Instant.parse("2026-08-02T18:00:59Z"));
        RateLimitService service = new RateLimitService(clock);

        service.checkCooldown("auth:resend", Duration.ofSeconds(60));
        clock.advance(Duration.ofSeconds(1));

        RateLimitException exception = assertThrows(
                RateLimitException.class,
                () -> service.checkCooldown("auth:resend", Duration.ofSeconds(60)));

        assertEquals(59, exception.getRetryAfterSeconds());

        clock.advance(Duration.ofSeconds(59));
        assertDoesNotThrow(() -> service.checkCooldown("auth:resend", Duration.ofSeconds(60)));
    }

    private static final class MutableClock extends Clock {

        private Instant instant;

        private MutableClock(Instant instant) {
            this.instant = instant;
        }

        private void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
