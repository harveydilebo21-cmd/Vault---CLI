package com.harvey.vault.clipboard;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Collects scheduled tasks; nothing runs until a test runs it. */
public class ManualScheduler implements Scheduler {

    public final List<Runnable> tasks = new ArrayList<>();
    public final List<Duration> delays = new ArrayList<>();

    @Override
    public void schedule(Runnable task, Duration delay) {
        tasks.add(task);
        delays.add(delay);
    }

    /** Runs every pending task once, as if the delay had passed. */
    public void runAll() {
        List<Runnable> pending = new ArrayList<>(tasks);
        tasks.clear();
        pending.forEach(Runnable::run);
    }
}
