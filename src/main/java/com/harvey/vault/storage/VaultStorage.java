package com.harvey.vault.storage;

import com.harvey.vault.crypto.CryptoService;
import com.harvey.vault.crypto.VaultDecryptionException;
import com.harvey.vault.model.Vault;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
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
        Path temp = null;
        try {
            // same folder as the target, so the final rename stays on one disk
            Path folder = file.toAbsolutePath().getParent();
            temp = Files.createTempFile(folder, "vault-", ".tmp");

            byte[] encrypted = crypto.encrypt(plain, password);
            writeBytes(temp, encrypted);
            restrictToOwner(temp);
            moveIntoPlace(temp, file);
        } catch (IOException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("Could not encrypt vault", e);
        } finally {
            Arrays.fill(plain, (byte) 0); // wipe the plaintext JSON
            if (temp != null) {
                Files.deleteIfExists(temp); // no-op after a successful move
            }
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
                Arrays.fill(plain, (byte) 0);
            }
        }
    }

    /** Package-private so tests can simulate a crash during the write. */
    void writeBytes(Path path, byte[] data) throws IOException {
        // SYNC forces the data onto the disk before we return
        Files.write(path, data, StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING, StandardOpenOption.SYNC);
    }

    private static void moveIntoPlace(Path temp, Path target) throws IOException {
        try {
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            // rare: file system can't do an atomic rename, fall back
            Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
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