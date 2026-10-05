package com.harvey.vault.model;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class EntryTest {

    @Test
    void blankSiteIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> new Entry("   ", "user", "pw"));
    }

    @Test
    void nullPasswordIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new Entry("github", "user", null));
    }

    @Test
    void siteIsTrimmed() {
        assertEquals("github", new Entry("  github ", "user", "pw").site());
    }

    @Test
    void toStringDoesNotRevealThePassword() {
        Entry e = new Entry("github", "harvey", "hunter2");
        String text = e.toString();
        assertFalse(text.contains("hunter2"));
        assertTrue(text.contains("github"));
    }
}