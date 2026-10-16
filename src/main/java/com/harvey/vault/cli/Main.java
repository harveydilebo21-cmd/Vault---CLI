package com.harvey.vault.cli;

import com.harvey.vault.crypto.VaultDecryptionException;
import com.harvey.vault.generator.PasswordGenerator;
import com.harvey.vault.model.Vault;
import com.harvey.vault.policy.MasterPasswordPolicy;
import com.harvey.vault.storage.VaultFormatException;
import com.harvey.vault.storage.VaultStorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
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
                terminal.println("Tip: a long passphrase of several words is stronger "
                        + "than a short complicated password.");
                password = new MasterPasswordSetup(terminal, new MasterPasswordPolicy()).choose();
                if (password == null) {
                    return;
                }
                vault = new Vault();
                storage.save(vault, file, password);
            }

            AutoLock lock = new AutoLock(timeout(), System::nanoTime);
            new VaultCli(terminal, storage, new PasswordGenerator(), file, vault, password, lock).run();
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

    /** Idle timeout: 5 minutes, or -Dvault.timeout.seconds=N for testing. */
    private static Duration timeout() {
        String seconds = System.getProperty("vault.timeout.seconds");
        if (seconds != null) {
            try {
                long value = Long.parseLong(seconds);
                if (value > 0) {
                    return Duration.ofSeconds(value);
                }
            } catch (NumberFormatException ignored) {
                // fall through to the default
            }
        }
        return AutoLock.DEFAULT_TIMEOUT;
    }

    private static boolean isEmpty(char[] chars) {
        return chars == null || chars.length == 0;
    }
}