package com.harvey.vault.clipboard;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class ClipboardGuardTest {

    private final FakeClipboard clipboard = new FakeClipboard();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final ClipboardGuard guard =
            new ClipboardGuard(clipboard, scheduler, Duration.ofSeconds(20));

    @Test
    void copyPutsTheTextOnTheClipboard() {
        assertTrue(guard.copy("secret"));
        assertEquals("secret", clipboard.content);
    }

    @Test
    void copySchedulesAClearAfterTheConfiguredDelay() {
        guard.copy("secret");
        assertEquals(1, scheduler.tasks.size());
        assertEquals(Duration.ofSeconds(20), scheduler.delays.get(0));
    }

    @Test
    void theScheduledClearEmptiesTheClipboard() {
        guard.copy("secret");
        scheduler.runAll();
        assertEquals("", clipboard.content);
    }

    @Test
    void theScheduledClearLeavesSomethingElseTheUserCopiedAlone() {
        guard.copy("secret");
        clipboard.content = "my shopping list";
        scheduler.runAll();
        assertEquals("my shopping list", clipboard.content);
    }

    @Test
    void anUnavailableClipboardReturnsFalseAndSchedulesNothing() {
        clipboard.available = false;
        assertFalse(guard.copy("secret"));
        assertTrue(scheduler.tasks.isEmpty());
        assertNull(clipboard.content);
    }

    @Test
    void clearNowClearsImmediately() {
        guard.copy("secret");
        guard.clearNow();
        assertEquals("", clipboard.content);
    }

    @Test
    void clearNowWithNothingCopiedDoesNothing() {
        clipboard.content = "something else";
        guard.clearNow();
        assertEquals("something else", clipboard.content);
    }

    @Test
    void theFirstTimerDoesNotClearASecondCopyOfTheSamePassword() {
        guard.copy("secret");
        guard.copy("secret");
        scheduler.tasks.get(0).run();          // the first timer fires
        assertEquals("secret", clipboard.content);
        scheduler.tasks.get(1).run();          // the second timer fires
        assertEquals("", clipboard.content);
    }

    @Test
    void aNewerCopyReplacesAnOlderOneAndOnlyTheNewerTimerClears() {
        guard.copy("one");
        guard.copy("two");
        assertEquals("two", clipboard.content);
        scheduler.tasks.get(0).run();
        assertEquals("two", clipboard.content);
        scheduler.tasks.get(1).run();
        assertEquals("", clipboard.content);
    }

    @Test
    void clearingTwiceIsHarmless() {
        guard.copy("secret");
        guard.clearNow();
        clipboard.content = "x";
        guard.clearNow();
        assertEquals("x", clipboard.content);
    }

    @Test
    void aNonPositiveDelayIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new ClipboardGuard(clipboard, scheduler, Duration.ZERO));
        assertThrows(IllegalArgumentException.class,
                () -> new ClipboardGuard(clipboard, scheduler, null));
    }
}