package com.harvey.vault.storage;

import com.harvey.vault.crypto.VaultDecryptionException;
import com.harvey.vault.model.Entry;
import com.harvey.vault.model.Vault;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class VaultStorageTest {

    @TempDir
    Path dir;

    private final VaultStorage storage = new VaultStorage();
    private final char[] password = "master-pw".toCharArray();
    private Path file;

    @BeforeEach
    void setUp() {
        file = dir.resolve("test.vault");
    }

    private static Vault sampleVault() {
        Vault v = new Vault();
        v.add(new Entry("github", "harvey", "hunter2"));
        v.add(new Entry("zoom", "harvey2", "p@ss2"));
        return v;
    }

    @Test
    void saveThenLoadReturnsTheSameEntries() throws Exception {
        Vault original = sampleVault();
        storage.save(original, file, password);
        Vault loaded = storage.load(file, password);
        assertEquals(original.all(), loaded.all());
    }

    @Test
    void fileOnDiskDoesNotContainPlaintext() throws Exception {
        storage.save(sampleVault(), file, password);
        String raw = new String(Files.readAllBytes(file), StandardCharsets.ISO_8859_1);
        assertFalse(raw.contains("hunter2"));
        assertFalse(raw.contains("github"));
        assertFalse(raw.contains("password"));
    }

    @Test
    void savingTwiceOverwritesThePreviousVault() throws Exception {
        storage.save(sampleVault(), file, password);
        Vault second = new Vault();
        second.add(new Entry("gitlab", "u", "p"));
        storage.save(second, file, password);

        Vault loaded = storage.load(file, password);
        assertEquals(second.all(), loaded.all());
    }

    @Test
    void savingTheSameVaultTwiceProducesDifferentFiles() throws Exception {
        Path other = dir.resolve("other.vault");
        storage.save(sampleVault(), file, password);
        storage.save(sampleVault(), other, password);
        assertFalse(java.util.Arrays.equals(
                Files.readAllBytes(file), Files.readAllBytes(other)));
    }

    @Test
    void loadWithWrongPasswordIsRejected() throws Exception {
        storage.save(sampleVault(), file, password);
        assertThrows(VaultDecryptionException.class,
                () -> storage.load(file, "wrong-pw".toCharArray()));
    }

    @Test
    void loadOfTamperedFileIsRejected() throws Exception {
        storage.save(sampleVault(), file, password);
        byte[] bytes = Files.readAllBytes(file);
        bytes[bytes.length - 1] ^= 1;
        Files.write(file, bytes);
        assertThrows(VaultDecryptionException.class,
                () -> storage.load(file, password));
    }

    @Test
    void loadOfEmptyFileIsRejected() throws Exception {
        Files.write(file, new byte[0]);
        assertThrows(VaultDecryptionException.class,
                () -> storage.load(file, password));
    }

    @Test
    void loadOfMissingFileThrowsIOException() {
        assertThrows(IOException.class,
                () -> storage.load(dir.resolve("nope.vault"), password));
    }

    @Test
    void fileIsReadableOnlyByOwnerOnPosixSystems() throws Exception {
        assumeTrue(FileSystems.getDefault()
                .supportedFileAttributeViews().contains("posix"));
        storage.save(sampleVault(), file, password);
        assertEquals(PosixFilePermissions.fromString("rw-------"),
                Files.getPosixFilePermissions(file));
    }
}