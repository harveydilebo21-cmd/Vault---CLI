package com.harvey.vault.cli;

import java.time.Duration;
import java.util.function.LongSupplier;

public class AutoLock {

    public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

    private final long timeoutNanos;
    private final LongSupplier nanoTime;
    private long lastActivity;

    public AutoLock(Duration timeout, LongSupplier nanoTime) {
        if (timeout == null || timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("Timeout must be positive");
        }
        this.timeoutNanos = timeout.toNanos();
        this.nanoTime = nanoTime;
        this.lastActivity = nanoTime.getAsLong();
    }

    public static AutoLock withDefaults() {
        return new AutoLock(DEFAULT_TIMEOUT, System::nanoTime);
    }

    /** Records that the user did something. */
    public void touch() {
        lastActivity = nanoTime.getAsLong();
    }

    public boolean isExpired() {
        // subtraction (not comparison) stays correct if nanoTime wraps around
        return nanoTime.getAsLong() - lastActivity >= timeoutNanos;
    }
}