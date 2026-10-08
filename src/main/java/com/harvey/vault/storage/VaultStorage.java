package com.harvey.vault.storage;

import com.harvey.vault.crypto.CryptoService;
import com.harvey.vault.crypto.VaultDecryptionException;
import com.harvey.vault.model.Vault;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Arrays;

public class VaultStorage {

    private final CryptoService crypto;
    private final VaultSerializer serializer;

    public VaultStorage() {
        this(new CryptoService(), new VaultSerializer());
    }

    public VaultStorage(CryptoService crypto, VaultSerializer serializer) {
        this.crypto = crypto;
        this.serializer = serializer;
    }

    public void save(Vault vault, Path file, char[] password) throws IOException {
        byte[] plain = serializer.toBytes(vault);
        try {
            byte[] encrypted = crypto.encrypt(plain, password);
            Files.write(file, encrypted);
            restrictToOwner(file);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt vault", e);
        } finally {
            Arrays.fill(plain, (byte) 0); // wipe the plaintext JSON
        }
    }

    public Vault load(Path file, char[] password)
            throws IOException, VaultDecryptionException, VaultFormatException {
        byte[] encrypted = Files.readAllBytes(file);
        byte[] plain = null;
        try {
            plain = crypto.decrypt(encrypted, password);
            return serializer.fromBytes(plain);
        } catch (VaultDecryptionException | VaultFormatException e) {
            throw e;
        } catch (IllegalArgumentException e) {
            // file too short to be a vault (empty, truncated)
            throw new VaultDecryptionException(
                    "Wrong password, or the vault file has been modified");
        } catch (Exception e) {
            throw new IllegalStateException("Could not decrypt vault", e);
        } finally {
            if (plain != null) {
                Arrays.fill(plain, (byte) 0); // wipe the plaintext JSON
            }
        }
    }

    private static void restrictToOwner(Path file) throws IOException {
        try {
            Files.setPosixFilePermissions(file,
                    PosixFilePermissions.fromString("rw-------"));
        } catch (UnsupportedOperationException e) {
            // not a POSIX file system (e.g. Windows): rely on the user's profile
        }
    }
}