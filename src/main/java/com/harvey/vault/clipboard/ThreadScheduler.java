package com.harvey.vault.clipboard;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ThreadScheduler implements Scheduler {

    // daemon thread, so a pending timer never keeps the program alive
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                Thread thread = new Thread(runnable, "clipboard-clear");
                thread.setDaemon(true);
                return thread;
            });

    @Override
    public void schedule(Runnable task, Duration delay) {
        executor.schedule(task, delay.toMillis(), TimeUnit.MILLISECONDS);
    }
}