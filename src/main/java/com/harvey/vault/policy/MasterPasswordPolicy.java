package com.harvey.vault.policy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

public class MasterPasswordPolicy {

    public static final int MIN_LENGTH = 15;
    public static final int MAX_LENGTH = 128;

    // Letters only, lowercase. Compared against the password after
    // trailing digits/symbols are stripped and look-alike characters swapped.
    private static final List<char[]> COMMON = Stream.of(
            "password", "passwort", "letmein", "welcome", "iloveyou", "admin",
            "administrator", "qwerty", "qwertyuiop", "asdfghjkl", "zxcvbnm",
            "monkey", "dragon", "football", "baseball", "superman", "batman",
            "trustno", "sunshine", "princess", "master", "masterpassword",
            "mypassword", "secret", "login", "whatever", "freedom", "shadow",
            "hello", "helloworld", "changeme", "starwars", "computer",
            "vault", "vaultcli", "abcdef"
    ).map(String::toCharArray).toList();

    private static final List<String> SEQUENCES = List.of(
            "01234567890123456789",
            "98765432109876543210",
            "abcdefghijklmnopqrstuvwxyz",
            "zyxwvutsrqponmlkjihgfedcba",
            "qwertyuiopasdfghjklzxcvbnm"
    );

    /** Returns the reasons the password is not acceptable. Empty means acceptable. */
    public List<String> check(char[] password) {
        List<String> problems = new ArrayList<>();
        int length = password == null ? 0 : password.length;

        if (length < MIN_LENGTH) {
            problems.add("Must be at least " + MIN_LENGTH + " characters long.");
        }
        if (length > MAX_LENGTH) {
            problems.add("Must be at most " + MAX_LENGTH + " characters long.");
        }
        if (length == 0) {
            return problems;
        }

        char[] lower = lowerCase(password);
        try {
            if (isRepeatedCharacter(lower)) {
                problems.add("Is a single repeated character.");
            }
            if (isSequence(lower)) {
                problems.add("Is a simple sequence (like 123456789012 or abcdefghijkl).");
            }
            if (isCommon(lower)) {
                problems.add("Is a common password, or a simple variation of one.");
            }
        } finally {
            Arrays.fill(lower, '\0'); // wipe our copy
        }
        return problems;
    }

    private static char[] lowerCase(char[] password) {
        char[] out = new char[password.length];
        for (int i = 0; i < password.length; i++) {
            out[i] = Character.toLowerCase(password[i]);
        }
        return out;
    }

    private static boolean isRepeatedCharacter(char[] lower) {
        if (lower.length < 2) {
            return false;
        }
        for (char c : lower) {
            if (c != lower[0]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isSequence(char[] lower) {
        for (String sequence : SEQUENCES) {
            if (isRunOf(sequence, lower)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isRunOf(String sequence, char[] lower) {
        int start = sequence.indexOf(lower[0]);
        if (start < 0 || start + lower.length > sequence.length()) {
            return false;
        }
        for (int i = 0; i < lower.length; i++) {
            if (sequence.charAt(start + i) != lower[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isCommon(char[] lower) {
        int end = lengthWithoutTrailingNonLetters(lower);
        char[] swapped = swapLookAlikes(lower);
        try {
            int swappedEnd = lengthWithoutTrailingNonLetters(swapped);
            for (char[] common : COMMON) {
                if (matches(lower, end, common)
                        || matches(swapped, end, common)
                        || matches(swapped, swappedEnd, common)) {
                    return true;
                }
            }
            return false;
        } finally {
            Arrays.fill(swapped, '\0');
        }
    }

    private static int lengthWithoutTrailingNonLetters(char[] text) {
        int end = text.length;
        while (end > 0 && !Character.isLetter(text[end - 1])) {
            end--;
        }
        return end;
    }

    private static char[] swapLookAlikes(char[] lower) {
        char[] out = new char[lower.length];
        for (int i = 0; i < lower.length; i++) {
            out[i] = switch (lower[i]) {
                case '@', '4' -> 'a';
                case '0' -> 'o';
                case '1' -> 'i';
                case '3' -> 'e';
                case '5', '$' -> 's';
                case '7' -> 't';
                default -> lower[i];
            };
        }
        return out;
    }

    private static boolean matches(char[] text, int length, char[] common) {
        return length == common.length
                && Arrays.equals(text, 0, length, common, 0, length);
    }
}