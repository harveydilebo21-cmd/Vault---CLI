package com.harvey.vault.policy;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MasterPasswordPolicyTest {

    private final MasterPasswordPolicy policy = new MasterPasswordPolicy();

    private List<String> problems(String password) {
        return policy.check(password.toCharArray());
    }

    private static boolean mentions(List<String> problems, String text) {
        return problems.stream().anyMatch(p -> p.contains(text));
    }

    // ---- what is accepted ----

    @Test
    void aPassphraseIsAccepted() {
        assertTrue(problems("correct horse battery staple").isEmpty());
    }

    @Test
    void aRandomMixedPasswordIsAccepted() {
        assertTrue(problems("k7#Qm2$vPz9xLw4").isEmpty());
    }

    @Test
    void lowercaseOnlyIsAcceptedBecauseThereAreNoCompositionRules() {
        assertTrue(problems("alllowercasewordsonlyhere").isEmpty());
    }

    @Test
    void spacesAndUnicodeAreAccepted() {
        assertTrue(problems("pässwörd mit ümlauts 42").isEmpty());
    }

    // ---- length ----

    @Test
    void fourteenCharactersIsTooShort() {
        assertTrue(mentions(problems("k7#Qm2$vPz9xLw"), "at least 15"));
    }

    @Test
    void emptyAndNullAreRejected() {
        assertTrue(mentions(problems(""), "at least 15"));
        assertTrue(mentions(policy.check(null), "at least 15"));
    }

    @Test
    void moreThan128CharactersIsRejected() {
        assertTrue(mentions(problems("Zq9!".repeat(33)), "at most 128")); // 132 chars
    }

    @Test
    void exactly128CharactersIsAccepted() {
        assertTrue(problems("Zq9!".repeat(32)).isEmpty()); // 128 chars
    }

    // ---- weak patterns ----

    @Test
    void commonPasswordsAreRejected() {
        String[] common = {
                "password1234567", "Password123456!!", "qwerty1234567890",
                "iloveyou2024!!!!", "letmein12345678", "administrator123",
                "changeme12345678"
        };
        for (String pw : common) {
            assertTrue(mentions(problems(pw), "common"), pw + " should be rejected as common");
        }
    }

    @Test
    void leetspeakVariantsOfCommonPasswordsAreRejected() {
        String[] disguised = {"P@ssw0rd!!!!!!!!", "pa55w0rd12345678", "W3lc0me12345678!"};
        for (String pw : disguised) {
            assertTrue(mentions(problems(pw), "common"), pw + " should be rejected as common");
        }
    }

    @Test
    void aSingleRepeatedCharacterIsRejected() {
        assertTrue(mentions(problems("aaaaaaaaaaaaaaaa"), "repeated"));
    }

    @Test
    void simpleSequencesAreRejected() {
        String[] sequences = {"abcdefghijklmnop", "1234567890123456", "ZYXWVUTSRQPONMLK", "qwertyuiopasdfgh"};
        for (String pw : sequences) {
            assertTrue(mentions(problems(pw), "sequence"), pw + " should be rejected as a sequence");
        }
    }

    // ---- behaviour ----

    @Test
    void severalProblemsAreAllReported() {
        assertTrue(problems("aaaaaaaa").size() >= 2); // too short AND repeated
    }

    @Test
    void checkDoesNotModifyTheCallersArray() {
        char[] password = "k7#Qm2$vPz9xLw4".toCharArray();
        char[] copy = password.clone();
        policy.check(password);
        assertArrayEquals(copy, password);
    }
}