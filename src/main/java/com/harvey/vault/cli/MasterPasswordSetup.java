package com.harvey.vault.cli;

import com.harvey.vault.policy.MasterPasswordPolicy;

import java.util.Arrays;
import java.util.List;

public class MasterPasswordSetup {

    static final int MAX_ATTEMPTS = 3;

    private final Terminal terminal;
    private final MasterPasswordPolicy policy;

    public MasterPasswordSetup(Terminal terminal, MasterPasswordPolicy policy) {
        this.terminal = terminal;
        this.policy = policy;
    }

    /** Returns the chosen master password, or null if none was set. */
    public char[] choose() {
        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            char[] password = terminal.readSecret("Choose a master password: ");
            if (password == null) {
                return null; // input ended
            }

            List<String> problems = policy.check(password);
            if (!problems.isEmpty()) {
                terminal.println("That master password is not acceptable:");
                for (String problem : problems) {
                    terminal.println(" - " + problem);
                }
                Arrays.fill(password, '\0');
                continue;
            }

            char[] repeat = terminal.readSecret("Repeat master password: ");
            try {
                if (repeat != null && Arrays.equals(password, repeat)) {
                    return password;
                }
            } finally {
                if (repeat != null) {
                    Arrays.fill(repeat, '\0');
                }
            }
            terminal.println("The passwords did not match.");
            Arrays.fill(password, '\0');
        }
        terminal.println("Too many attempts. No vault was created.");
        return null;
    }
}