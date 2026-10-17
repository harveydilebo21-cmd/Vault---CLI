package com.harvey.vault.clipboard;

import java.time.Duration;

public interface Scheduler {

    void schedule(Runnable task, Duration delay);
}