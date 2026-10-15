package com.harvey.vault.cli;

import com.harvey.vault.policy.MasterPasswordPolicy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MasterPasswordSetupTest {

    private static final String STRONG = "correct horse battery staple";

    private ScriptedTerminal terminal;

    private char[] choose(String... inputs) {
        terminal = new ScriptedTerminal(inputs);
        return new MasterPasswordSetup(terminal, new MasterPasswordPolicy()).choose();
    }

    @Test
    void aStrongPasswordTypedTwiceIsAccepted() {
        assertArrayEquals(STRONG.toCharArray(), choose(STRONG, STRONG));
    }

    @Test
    void aWeakPasswordIsRejectedWithReasonsThenAStrongOneIsAccepted() {
        char[] result = choose("password", STRONG, STRONG);
        assertArrayEquals(STRONG.toCharArray(), result);
        assertTrue(terminal.output().contains("not acceptable"));
        assertTrue(terminal.output().contains("at least 15"));
    }

    @Test
    void aMismatchedRepeatIsRejectedThenTheNextAttemptWorks() {
        char[] result = choose(STRONG, "correct horse battery stapler", STRONG, STRONG);
        assertArrayEquals(STRONG.toCharArray(), result);
        assertTrue(terminal.output().contains("did not match"));
    }

    @Test
    void givesUpAfterThreeFailedAttempts() {
        assertNull(choose("a", "b", "c"));
        assertTrue(terminal.output().contains("Too many attempts"));
    }

    @Test
    void aWeakPasswordIsNeverAskedToBeRepeated() {
        choose("a", "b", "c");
        assertFalse(terminal.output().contains("Repeat"));
    }

    @Test
    void endOfInputReturnsNull() {
        assertNull(choose());
    }

    @Test
    void neverPrintsThePassword() {
        choose(STRONG, STRONG);
        assertFalse(terminal.output().contains(STRONG));
    }
}