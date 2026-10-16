package com.harvey.vault.cli;

import java.time.Duration;
import java.util.function.LongSupplier;

/** A fake nanoTime source that only moves when a test says so. */
class FakeNanos implements LongSupplier {

    private long now;

    FakeNanos() {
        this(1_000_000_000L);
    }

    FakeNanos(long start) {
        this.now = start;
    }

    void advance(Duration duration) {
        now += duration.toNanos();
    }

    @Override
    public long getAsLong() {
        return now;
    }
}