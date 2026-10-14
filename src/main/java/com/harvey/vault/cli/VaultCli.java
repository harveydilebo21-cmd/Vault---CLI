package com.harvey.vault.cli;

import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import com.harvey.vault.storage.VaultStorage;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class VaultCli {

    private static final int DEFAULT_LENGTH = 16;

    private final Terminal terminal;
    private final VaultStorage storage;
    private final PasswordGenerator generator;
    private final Path file;
    private final Vault vault;
    private final char[] masterPassword; // needed to re-encrypt on every save

    public VaultCli(Terminal terminal, VaultStorage storage, PasswordGenerator generator,
                    Path file, Vault vault, char[] masterPassword) {
        this.terminal = terminal;
        this.storage = storage;
        this.generator = generator;
        this.file = file;
        this.vault = vault;
        this.masterPassword = masterPassword;
    }

    public void run() {
        while (true) {
            showMenu();
            String choice = terminal.readLine("> ");
            if (choice == null) {
                return; // input ended
            }
            switch (choice.trim()) {
                case "1" -> add();
                case "2" -> get();
                case "3" -> list();
                case "4" -> delete();
                case "5" -> generate();
                case "6" -> {
                    terminal.println("Goodbye.");
                    return;
                }
                default -> terminal.println("Unknown option.");
            }
        }
    }

    private void showMenu() {
        terminal.println("");
        terminal.println("1) Add entry");
        terminal.println("2) Get entry");
        terminal.println("3) List sites");
        terminal.println("4) Delete entry");
        terminal.println("5) Generate password");
        terminal.println("6) Quit");
    }

    private void add() {
        String site = ask("Site: ");
        if (site.isBlank()) {
            terminal.println("Site must not be blank.");
            return;
        }
        if (vault.get(site).isPresent()) {
            terminal.println("An entry for " + site + " already exists.");
            return;
        }
        String username = ask("Username: ");

        char[] typed = terminal.readSecret("Password (leave blank to generate one): ");
        String entryPassword;
        if (typed == null || typed.length == 0) {
            entryPassword = generator.generate(DEFAULT_LENGTH);
            terminal.println("Generated a " + DEFAULT_LENGTH + "-character password.");
        } else {
            entryPassword = new String(typed);
            Arrays.fill(typed, '\0');
        }

        vault.add(new Entry(site, username, entryPassword));
        if (save()) {
            terminal.println("Saved entry for " + site + ".");
        }
    }

    private void get() {
        String site = ask("Site: ");
        Optional<Entry> entry = vault.get(site);
        if (entry.isEmpty()) {
            terminal.println("No entry for site: " + site);
            return;
        }
        terminal.println("Username: " + entry.get().username());
        terminal.println("Password: " + entry.get().password());
    }

    private void list() {
        List<Entry> all = vault.all();
        if (all.isEmpty()) {
            terminal.println("The vault is empty.");
            return;
        }
        for (Entry e : all) {
            terminal.println("- " + e.site() + " (" + e.username() + ")");
        }
    }

    private void delete() {
        String site = ask("Site to delete: ");
        if (vault.get(site).isEmpty()) {
            terminal.println("No entry for site: " + site);
            return;
        }
        String answer = ask("Delete " + site + "? (y/N): ");
        if (!answer.equalsIgnoreCase("y")) {
            terminal.println("Cancelled.");
            return;
        }
        vault.delete(site);
        if (save()) {
            terminal.println("Deleted " + site + ".");
        }
    }

    private void generate() {
        String text = ask("Length (8-128, default 16): ");
        int length = DEFAULT_LENGTH;
        if (!text.isEmpty()) {
            try {
                length = Integer.parseInt(text);
            } catch (NumberFormatException e) {
                terminal.println(lengthMessage());
                return;
            }
        }
        try {
            terminal.println("Generated password: " + generator.generate(length));
        } catch (IllegalArgumentException e) {
            terminal.println(lengthMessage());
        }
    }

    private String lengthMessage() {
        return "Length must be between " + PasswordGenerator.MIN_LENGTH
                + " and " + PasswordGenerator.MAX_LENGTH;
    }

    private String ask(String prompt) {
        String answer = terminal.readLine(prompt);
        return answer == null ? "" : answer.trim();
    }

    private boolean save() {
        try {
            storage.save(vault, file, masterPassword);
            return true;
        } catch (IOException e) {
            terminal.println("Could not save the vault. Your last change is NOT on disk.");
            return false;
        }
    }
}