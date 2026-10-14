package com.harvey.vault.cli;

import com.harvey.vault.crypto.VaultDecryptionException;
import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Vault;
import com.harvey.vault.storage.VaultFormatException;
import com.harvey.vault.storage.VaultStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        Path file = Path.of(args.length > 0 ? args[0] : "vault.vault");
        Terminal terminal = new ConsoleTerminal();
        VaultStorage storage = new VaultStorage();
        char[] password = null;

        try {
            Vault vault;
            if (Files.exists(file)) {
                password = terminal.readSecret("Master password: ");
                if (isEmpty(password)) {
                    terminal.println("A master password is required.");
                    return;
                }
                vault = storage.load(file, password);
            } else {
                terminal.println("No vault found. Creating a new one at " + file);
                password = terminal.readSecret("Choose a master password: ");
                char[] repeat = terminal.readSecret("Repeat master password: ");
                try {
                    if (isEmpty(password) || !Arrays.equals(password, repeat)) {
                        terminal.println("Passwords were empty or did not match. Nothing created.");
                        return;
                    }
                } finally {
                    if (repeat != null) {
                        Arrays.fill(repeat, '\0');
                    }
                }
                vault = new Vault();
                storage.save(vault, file, password);
            }
            new VaultCli(terminal, storage, new PasswordGenerator(), file, vault, password).run();
        } catch (VaultDecryptionException | VaultFormatException e) {
            terminal.println("Could not open the vault: " + e.getMessage());
        } catch (IOException e) {
            terminal.println("Could not read or write the vault file.");
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0'); // wipe the master password on exit
            }
        }
    }

    private static boolean isEmpty(char[] chars) {
        return chars == null || chars.length == 0;
    }
}