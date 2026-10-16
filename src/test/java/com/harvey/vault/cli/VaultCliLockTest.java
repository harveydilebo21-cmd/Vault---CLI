package com.harvey.vault.cli;

import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import com.harvey.vault.storage.VaultStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VaultCliLockTest {

    @TempDir
    Path dir;

    private final VaultStorage storage = new VaultStorage();
    private final char[] password = "master-pw".toCharArray();
    private final FakeNanos nanos = new FakeNanos();
    private Path file;
    private Vault vault;

    @BeforeEach
    void setUp() throws Exception {
        file = dir.resolve("test.vault");
        vault = new Vault();
        vault.add(new Entry("github", "harvey", "p@ss1"));
        storage.save(vault, file, password); // unlocking checks the password against this file
    }

    private ScriptedTerminal run(Object... script) {
        ScriptedTerminal terminal = new ScriptedTerminal(script);
        AutoLock lock = new AutoLock(Duration.ofMinutes(5), nanos);
        new VaultCli(terminal, storage, new PasswordGenerator(), file, vault, password, lock).run();
        return terminal;
    }

    private Runnable idle(Duration duration) {
        return () -> nanos.advance(duration);
    }

    private static int count(String text, String part) {
        return text.split(Pattern.quote(part), -1).length - 1;
    }

    @Test
    void shortGapsBetweenCommandsNeverLockTheSession() {
        String out = run("3", idle(Duration.ofMinutes(4)), "3",
                idle(Duration.ofMinutes(4)), "3", "6").output();

        assertFalse(out.contains("Session locked"));
        assertEquals(3, count(out, "- github ("));
    }

    @Test
    void anIdleSessionAsksForTheMasterPasswordThenWorksAgain() {
        String out = run(idle(Duration.ofMinutes(6)), "3", "master-pw", "3", "6").output();

        assertTrue(out.contains("Session locked"));
        assertEquals(1, count(out, "- github ("));
    }

    @Test
    void aCommandTypedWhileLockedIsDiscarded() {
        String out = run(idle(Duration.ofMinutes(6)), "4", "master-pw", "6").output();

        assertFalse(out.contains("Site to delete"));
        assertTrue(vault.get("github").isPresent());
    }

    @Test
    void lockingClearsTheEntriesFromMemory() {
        run(idle(Duration.ofMinutes(6)), "3"); // nobody ever unlocks
        assertEquals(0, vault.size());
    }

    @Test
    void threeWrongPasswordsEndTheSession() {
        String out = run(idle(Duration.ofMinutes(6)), "3", "w1", "w2", "w3").output();

        assertTrue(out.contains("Too many failed attempts"));
        assertFalse(out.contains("- github ("));
        assertEquals(0, vault.size());
    }

    @Test
    void oneMistakeThenTheCorrectPasswordUnlocks() {
        String out = run(idle(Duration.ofMinutes(6)), "3", "wrong", "master-pw", "3", "6").output();

        assertTrue(out.contains("Could not unlock"));
        assertEquals(1, count(out, "- github ("));
    }

    @Test
    void unlockingRestoresTheEntries() {
        run(idle(Duration.ofMinutes(6)), "3", "master-pw", "6");
        assertEquals("p@ss1", vault.get("github").orElseThrow().password());
    }

    @Test
    void theTimerRestartsAfterUnlocking() {
        String out = run(idle(Duration.ofMinutes(6)), "3", "master-pw",
                idle(Duration.ofMinutes(4)), "3", "6").output();

        assertEquals(1, count(out, "Session locked"));
        assertEquals(1, count(out, "- github ("));
    }

    @Test
    void savingStillWorksAfterUnlocking() throws Exception {
        run(idle(Duration.ofMinutes(6)), "3", "master-pw", "1", "gitlab", "u", "p", "6");
        assertEquals(2, storage.load(file, password).size());
    }
}