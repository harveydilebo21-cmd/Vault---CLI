package com.harvey.vault.generator;

import com.harvey.vault.generator.PasswordGenerator.Options;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PasswordGeneratorTest {

    private final PasswordGenerator generator = new PasswordGenerator();

    private static String classOf(char c) {
        if (Character.isLowerCase(c)) return "lower";
        if (Character.isUpperCase(c)) return "upper";
        if (Character.isDigit(c)) return "digit";
        return "symbol";
    }

    // ---- length ----

    @Test
    void passwordHasTheRequestedLength() {
        for (int length : new int[]{8, 12, 16, 32, 128}) {
            assertEquals(length, generator.generate(length).length());
        }
    }

    @Test
    void tooShortLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(7));
    }

    @Test
    void tooLongLengthIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> generator.generate(129));
    }

    // ---- character classes ----

    @Test
    void defaultPasswordAlwaysContainsEveryClass() {
        for (int i = 0; i < 500; i++) {
            String pw = generator.generate(8); // shortest case is the hardest
            Set<String> classes = new HashSet<>();
            pw.chars().forEach(c -> classes.add(classOf((char) c)));
            assertEquals(4, classes.size(), "Missing a class in: " + pw);
        }
    }

    @Test
    void onlyRequestedClassesAreUsed() {
        String digits = generator.generate(32, new Options(false, false, true, false));
        assertTrue(digits.chars().allMatch(Character::isDigit));

        String lower = generator.generate(32, new Options(true, false, false, false));
        assertTrue(lower.chars().allMatch(Character::isLowerCase));
    }

    @Test
    void selectingNoClassIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> generator.generate(16, new Options(false, false, false, false)));
    }

    // ---- randomness ----

    @Test
    void passwordsDoNotRepeat() {
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < 1000; i++) {
            seen.add(generator.generate(16));
        }
        assertEquals(1000, seen.size());
    }

    @Test
    void firstCharacterIsNotAlwaysTheSameClass() {
        Set<String> firstClasses = new HashSet<>();
        for (int i = 0; i < 300; i++) {
            firstClasses.add(classOf(generator.generate(16).charAt(0)));
        }
        assertTrue(firstClasses.size() > 1, "Passwords start with a predictable class");
    }

    @Test
    void digitsAreRoughlyUniformlyDistributed() {
        int[] counts = new int[10];
        int total = 0;
        Options digitsOnly = new Options(false, false, true, false);
        for (int i = 0; i < 800; i++) {
            for (char c : generator.generate(128, digitsOnly).toCharArray()) {
                counts[c - '0']++;
                total++;
            }
        }
        for (int d = 0; d < 10; d++) {
            double share = (double) counts[d] / total;
            assertTrue(share > 0.08 && share < 0.12,
                    "Digit " + d + " appeared " + share * 100 + "% of the time");
        }
    }
}