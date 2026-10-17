package com.harvey.vault.cli;

import com.harvey.vault.clipboard.ClipboardGuard;
import com.harvey.vault.clipboard.FakeClipboard;
import com.harvey.vault.clipboard.ManualScheduler;
import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import com.harvey.vault.storage.VaultStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

class VaultCliClipboardTest {

    @TempDir
    Path dir;

    private final VaultStorage storage = new VaultStorage();
    private final char[] password = "master-pw".toCharArray();
    private final FakeClipboard clipboard = new FakeClipboard();
    private final ManualScheduler scheduler = new ManualScheduler();
    private final ClipboardGuard guard =
            new ClipboardGuard(clipboard, scheduler, Duration.ofSeconds(20));
    private final FakeNanos nanos = new FakeNanos();
    private final String[] seen = new String[1]; // what the clipboard held at a chosen moment
    private Path file;
    private Vault vault;

    @BeforeEach
    void setUp() {
        file = dir.resolve("test.vault");
        vault = new Vault();
        vault.add(new Entry("github", "harvey", "p@ss1"));
    }

    private ScriptedTerminal run(Object... script) {
        ScriptedTerminal terminal = new ScriptedTerminal(script);
        AutoLock lock = new AutoLock(Duration.ofMinutes(5), nanos);
        new VaultCli(terminal, storage, new PasswordGenerator(), file, vault, password, lock, guard).run();
        return terminal;
    }

    /** Records what is on the clipboard at this point in the script. */
    private Runnable capture() {
        return () -> seen[0] = clipboard.content;
    }

    private Runnable timerFires() {
        return scheduler::runAll;
    }

    private Runnable idleForSixMinutes() {
        return () -> nanos.advance(Duration.ofMinutes(6));
    }

    @Test
    void copyPutsThePasswordOnTheClipboardWithoutPrintingIt() {
        String out = run("c", "github", capture(), "6").output();

        assertEquals("p@ss1", seen[0]);
        assertTrue(out.contains("copied"));
        assertFalse(out.contains("p@ss1"));
    }

    @Test
    void anUppercaseCWorksToo() {
        run("C", "github", capture(), "6");
        assertEquals("p@ss1", seen[0]);
    }

    @Test
    void copyOfAnUnknownSiteLeavesTheClipboardAlone() {
        String out = run("c", "nothing", "6").output();

        assertTrue(out.contains("No entry for site: nothing"));
        assertNull(clipboard.content);
    }

    @Test
    void anUnavailableClipboardSaysSo() {
        clipboard.available = false;
        String out = run("c", "github", "6").output();

        assertTrue(out.contains("not available"));
        assertNull(clipboard.content);
    }

    @Test
    void quittingClearsTheClipboard() {
        run("c", "github", "6");
        assertEquals("", clipboard.content);
    }

    @Test
    void endOfInputClearsTheClipboard() {
        run("c", "github");
        assertEquals("", clipboard.content);
    }

    @Test
    void theTimerClearsTheClipboardWhileTheSessionContinues() {
        run("c", "github", timerFires(), capture(), "6");
        assertEquals("", seen[0]); // cleared by the timer, before we quit
    }

    @Test
    void lockingClearsTheClipboard() throws Exception {
        storage.save(vault, file, password); // unlocking checks the password against this file

        run("c", "github", idleForSixMinutes(), "3", capture(), "master-pw", "6");

        assertEquals("", seen[0]); // already empty by the time the unlock prompt appeared
    }

    @Test
    void theMenuListsTheCopyOption() {
        assertTrue(run("6").output().contains("Copy password to clipboard"));
    }
}