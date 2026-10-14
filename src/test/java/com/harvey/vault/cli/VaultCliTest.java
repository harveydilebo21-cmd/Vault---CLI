package com.harvey.vault.cli;

import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import com.harvey.vault.storage.VaultStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

class VaultCliTest {

    @TempDir
    Path dir;

    private final VaultStorage storage = new VaultStorage();
    private final char[] password = "master-pw".toCharArray();
    private Path file;
    private Vault vault;

    @BeforeEach
    void setUp() {
        file = dir.resolve("test.vault");
        vault = new Vault();
    }

    private ScriptedTerminal run(String... inputs) {
        ScriptedTerminal terminal = new ScriptedTerminal(inputs);
        new VaultCli(terminal, storage, new PasswordGenerator(), file, vault, password).run();
        return terminal;
    }

    // ---- menu basics ----

    @Test
    void quitSaysGoodbye() {
        assertTrue(run("6").output().contains("Goodbye."));
    }

    @Test
    void endOfInputExitsCleanly() {
        assertDoesNotThrow(() -> run());
    }

    @Test
    void unknownOptionShowsAMessage() {
        assertTrue(run("9", "6").output().contains("Unknown option."));
    }

    // ---- add ----

    @Test
    void addStoresTheEntryAndSavesItToDisk() throws Exception {
        run("1", "github", "harvey", "p@ss1", "6");

        Entry saved = storage.load(file, password).get("github").orElseThrow();
        assertEquals("harvey", saved.username());
        assertEquals("p@ss1", saved.password());
    }

    @Test
    void addWithBlankPasswordGeneratesOne() {
        run("1", "github", "harvey", "", "6");
        assertEquals(16, vault.get("github").orElseThrow().password().length());
    }

    @Test
    void addDoesNotEchoThePassword() {
        String out = run("1", "github", "harvey", "topsecret99", "6").output();
        assertFalse(out.contains("topsecret99"));
    }

    @Test
    void addRejectsABlankSite() {
        String out = run("1", "   ", "6").output();
        assertTrue(out.contains("must not be blank"));
        assertEquals(0, vault.size());
    }

    @Test
    void addRejectsADuplicateSiteAndKeepsTheOriginal() {
        vault.add(new Entry("github", "harvey", "old"));
        String out = run("1", "github", "6").output();
        assertTrue(out.contains("already exists"));
        assertEquals("old", vault.get("github").orElseThrow().password());
    }

    // ---- get ----

    @Test
    void getShowsUsernameAndPassword() {
        vault.add(new Entry("github", "harvey", "p@ss1"));
        String out = run("2", "github", "6").output();
        assertTrue(out.contains("Username: harvey"));
        assertTrue(out.contains("Password: p@ss1"));
    }

    @Test
    void getUnknownSiteSaysSo() {
        assertTrue(run("2", "nothing", "6").output().contains("No entry for site: nothing"));
    }

    // ---- list ----

    @Test
    void listShowsSitesButNeverPasswords() {
        vault.add(new Entry("github", "harvey", "hunter2"));
        vault.add(new Entry("zoom", "harvey2", "p@ss2"));
        String out = run("3", "6").output();
        assertTrue(out.contains("github"));
        assertTrue(out.contains("zoom"));
        assertFalse(out.contains("hunter2"));
        assertFalse(out.contains("p@ss2"));
    }

    @Test
    void listOfEmptyVaultSaysSo() {
        assertTrue(run("3", "6").output().contains("The vault is empty."));
    }

    // ---- delete ----

    @Test
    void deleteWithConfirmationRemovesTheEntryAndSaves() throws Exception {
        vault.add(new Entry("github", "harvey", "p@ss1"));
        run("4", "github", "y", "6");

        assertTrue(vault.get("github").isEmpty());
        assertEquals(0, storage.load(file, password).size());
    }

    @Test
    void deleteDeclinedKeepsTheEntry() {
        vault.add(new Entry("github", "harvey", "p@ss1"));
        run("4", "github", "n", "6");

        assertTrue(vault.get("github").isPresent());
        assertFalse(Files.exists(file)); // nothing was saved
    }

    @Test
    void deleteUnknownSiteSaysSo() {
        assertTrue(run("4", "nothing", "6").output().contains("No entry for site"));
    }

    // ---- generate ----

    @Test
    void generateProducesAPasswordOfTheRequestedLength() {
        String out = run("5", "20", "6").output();
        Matcher m = Pattern.compile("(?m)^Generated password: (\\S+)$").matcher(out);
        assertTrue(m.find());
        assertEquals(20, m.group(1).length());
    }

    @Test
    void generateRejectsAnInvalidLength() {
        assertTrue(run("5", "abc", "6").output().contains("between 8 and 128"));
        assertTrue(run("5", "3", "6").output().contains("between 8 and 128"));
    }
}