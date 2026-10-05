package com.harvey.vault.model;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.NoSuchElementException;
import static org.junit.jupiter.api.Assertions.*;

class VaultTest {

    private Vault vault;

    @BeforeEach
    void setUp() {
        vault = new Vault();
    }

    @Test
    void newVaultIsEmpty() {
        assertEquals(0, vault.size());
        assertTrue(vault.all().isEmpty());
    }

    @Test
    void addThenGetReturnsTheEntry() {
        vault.add(new Entry("github", "harvey", "p@ss"));
        assertEquals("p@ss", vault.get("github").orElseThrow().password());
    }

    @Test
    void getUnknownSiteReturnsEmpty() {
        assertTrue(vault.get("nothing").isEmpty());
    }

    @Test
    void duplicateSiteIsRejected() {
        vault.add(new Entry("github", "a", "b"));
        assertThrows(IllegalArgumentException.class,
                () -> vault.add(new Entry("github", "c", "d")));
    }

    @Test
    void lookupAndDuplicateCheckIgnoreCase() {
        vault.add(new Entry("GitHub", "a", "b"));
        assertTrue(vault.get("github").isPresent());
        assertThrows(IllegalArgumentException.class,
                () -> vault.add(new Entry("github", "c", "d")));
    }

    @Test
    void updateReplacesTheEntry() {
        vault.add(new Entry("github", "a", "old"));
        vault.update(new Entry("github", "a", "new"));
        assertEquals("new", vault.get("github").orElseThrow().password());
        assertEquals(1, vault.size());
    }

    @Test
    void updateUnknownSiteThrows() {
        assertThrows(NoSuchElementException.class,
                () -> vault.update(new Entry("github", "a", "b")));
    }

    @Test
    void deleteRemovesTheEntry() {
        vault.add(new Entry("github", "a", "b"));
        assertTrue(vault.delete("github"));
        assertTrue(vault.get("github").isEmpty());
    }

    @Test
    void deleteUnknownSiteReturnsFalse() {
        assertFalse(vault.delete("nothing"));
    }

    @Test
    void allIsSortedBySite() {
        vault.add(new Entry("zoom", "a", "b"));
        vault.add(new Entry("amazon", "a", "b"));
        List<String> sites = vault.all().stream().map(Entry::site).toList();
        assertEquals(List.of("amazon", "zoom"), sites);
    }

    @Test
    void allCannotBeUsedToModifyTheVault() {
        vault.add(new Entry("github", "a", "b"));
        assertThrows(UnsupportedOperationException.class,
                () -> vault.all().add(new Entry("evil", "a", "b")));
        assertEquals(1, vault.size());
    }
}