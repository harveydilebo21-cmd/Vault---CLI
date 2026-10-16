package com.harvey.vault.cli;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class AutoLockTest {

    private static final Duration FIVE_MINUTES = Duration.ofMinutes(5);

    private final FakeNanos nanos = new FakeNanos();
    private final AutoLock lock = new AutoLock(FIVE_MINUTES, nanos);

    @Test
    void isNotExpiredAtTheStart() {
        assertFalse(lock.isExpired());
    }

    @Test
    void isNotExpiredJustBeforeTheTimeout() {
        nanos.advance(Duration.ofMinutes(4).plusSeconds(59));
        assertFalse(lock.isExpired());
    }

    @Test
    void isExpiredExactlyAtTheTimeout() {
        nanos.advance(FIVE_MINUTES);
        assertTrue(lock.isExpired());
    }

    @Test
    void touchRestartsTheTimer() {
        nanos.advance(Duration.ofMinutes(4));
        lock.touch();
        nanos.advance(Duration.ofMinutes(4));
        assertFalse(lock.isExpired()); // 8 minutes in total, but only 4 since the touch
    }

    @Test
    void touchAfterExpiryStartsAFreshPeriod() {
        nanos.advance(Duration.ofMinutes(6));
        assertTrue(lock.isExpired());
        lock.touch();
        assertFalse(lock.isExpired());
    }

    @Test
    void aNonPositiveOrMissingTimeoutIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new AutoLock(Duration.ZERO, nanos));
        assertThrows(IllegalArgumentException.class, () -> new AutoLock(Duration.ofSeconds(-1), nanos));
        assertThrows(IllegalArgumentException.class, () -> new AutoLock(null, nanos));
    }

    @Test
    void worksWhenNanoTimeWrapsAround() {
        FakeNanos nearTheEnd = new FakeNanos(Long.MAX_VALUE - 1000);
        AutoLock wrapping = new AutoLock(FIVE_MINUTES, nearTheEnd);
        nearTheEnd.advance(Duration.ofMinutes(6)); // the counter overflows to a negative number
        assertTrue(wrapping.isExpired());
    }
}