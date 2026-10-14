package com.harvey.vault.generator;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class PasswordGenerator {

    public static final int MIN_LENGTH = 8;
    public static final int MAX_LENGTH = 128;

    private static final String LOWER = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String DIGITS = "0123456789";
    private static final String SYMBOLS = "!@#$%^&*()-_=+[]{};:,.?";

    public record Options(boolean lower, boolean upper, boolean digits, boolean symbols) {
        public static Options all() {
            return new Options(true, true, true, true);
        }
    }

    private final SecureRandom random = new SecureRandom();

    public String generate(int length) {
        return generate(length, Options.all());
    }

    public String generate(int length, Options options) {
        if (length < MIN_LENGTH || length > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "Length must be between " + MIN_LENGTH + " and " + MAX_LENGTH);
        }
        List<String> sets = selectedSets(options);
        if (sets.isEmpty()) {
            throw new IllegalArgumentException("Select at least one character class");
        }

        StringBuilder pool = new StringBuilder();
        List<Character> chars = new ArrayList<>(length);

        // one guaranteed character from every selected class
        for (String set : sets) {
            pool.append(set);
            chars.add(pick(set));
        }
        // fill the rest from the combined pool
        while (chars.size() < length) {
            chars.add(pick(pool));
        }
        // shuffle so the guaranteed characters don't always come first
        Collections.shuffle(chars, random);

        StringBuilder out = new StringBuilder(length);
        for (char c : chars) {
            out.append(c);
        }
        return out.toString();
    }

    private List<String> selectedSets(Options o) {
        List<String> sets = new ArrayList<>();
        if (o.lower()) sets.add(LOWER);
        if (o.upper()) sets.add(UPPER);
        if (o.digits()) sets.add(DIGITS);
        if (o.symbols()) sets.add(SYMBOLS);
        return sets;
    }

    private char pick(CharSequence set) {
        return set.charAt(random.nextInt(set.length()));
    }
}