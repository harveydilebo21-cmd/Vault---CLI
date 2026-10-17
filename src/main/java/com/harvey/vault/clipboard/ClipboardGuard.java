package com.harvey.vault.clipboard;

import java.time.Duration;

public class ClipboardGuard {

    public static final Duration DEFAULT_CLEAR_AFTER = Duration.ofSeconds(20);

    private final ClipboardAccess clipboard;
    private final Scheduler scheduler;
    private final Duration clearAfter;

    private String lastCopied; // what we put on the clipboard, or null
    private long copyNumber;   // which copy the pending timer belongs to

    public ClipboardGuard(ClipboardAccess clipboard, Scheduler scheduler, Duration clearAfter) {
        if (clearAfter == null || clearAfter.isZero() || clearAfter.isNegative()) {
            throw new IllegalArgumentException("Clear delay must be positive");
        }
        this.clipboard = clipboard;
        this.scheduler = scheduler;
        this.clearAfter = clearAfter;
    }

    /** A guard that never touches a clipboard. Used where no clipboard is wanted. */
    public static ClipboardGuard disabled() {
        ClipboardAccess none = new ClipboardAccess() {
            @Override
            public boolean isAvailable() {
                return false;
            }

            @Override
            public void write(String text) {
            }

            @Override
            public String read() {
                return null;
            }
        };
        return new ClipboardGuard(none, (task, delay) -> { }, DEFAULT_CLEAR_AFTER);
    }

    public Duration clearAfter() {
        return clearAfter;
    }

    /** Copies the text and schedules the clipboard to be cleared. False if there is no clipboard. */
    public synchronized boolean copy(String text) {
        if (!clipboard.isAvailable()) {
            return false;
        }
        clipboard.write(text);
        lastCopied = text;
        long mine = ++copyNumber;
        scheduler.schedule(() -> clearIfUnchanged(mine), clearAfter);
        return true;
    }

    /** Clears the clipboard now, but only if it still holds what we copied. */
    public synchronized void clearNow() {
        if (lastCopied == null) {
            return;
        }
        if (lastCopied.equals(clipboard.read())) {
            clipboard.write("");
        }
        lastCopied = null;
    }

    private synchronized void clearIfUnchanged(long timerCopyNumber) {
        if (timerCopyNumber == copyNumber) { // ignore timers from older copies
            clearNow();
        }
    }
}