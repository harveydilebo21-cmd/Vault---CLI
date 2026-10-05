package com.harvey.vault.crypto;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

class CryptoServiceTest {

    private final CryptoService crypto = new CryptoService();
    private final char[] password = "master-pw".toCharArray();

    @Test
    void encryptThenDecryptReturnsOriginal() throws Exception {
        byte[] plain = "secret data".getBytes(StandardCharsets.UTF_8);
        byte[] blob = crypto.encrypt(plain, password);
        assertArrayEquals(plain, crypto.decrypt(blob, password));
    }

    @Test
    void emptyPlaintextRoundTrips() throws Exception {
        byte[] blob = crypto.encrypt(new byte[0], password);
        assertArrayEquals(new byte[0], crypto.decrypt(blob, password));
    }

    @Test
    void sameInputProducesDifferentOutputEachTime() throws Exception {
        byte[] plain = "x".getBytes(StandardCharsets.UTF_8);
        byte[] a = crypto.encrypt(plain, password);
        byte[] b = crypto.encrypt(plain, password);
        assertFalse(Arrays.equals(a, b)); // fresh salt and IV every time
    }

    @Test
    void blobHasExpectedLayoutSize() throws Exception {
        byte[] plain = new byte[100];
        byte[] blob = crypto.encrypt(plain, password);
        // salt (16) + IV (12) + ciphertext (same length as plaintext) + tag (16)
        assertEquals(16 + 12 + 100 + 16, blob.length);
    }

    @Test
    void tooShortBlobIsRejected() {
        assertThrows(IllegalArgumentException.class,
                () -> crypto.decrypt(new byte[10], password));
    }
    private byte[] sampleBlob() throws Exception {
        return crypto.encrypt("secret".getBytes(StandardCharsets.UTF_8), password);
    }

    @Test
    void wrongPasswordIsRejected() throws Exception {
        byte[] blob = sampleBlob();
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(blob, "wrong-pw".toCharArray()));
    }

    @Test
    void tamperedSaltIsRejected() throws Exception {
        byte[] bad = sampleBlob();
        bad[0] ^= 1; // flip one bit in the salt
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));
    }

    @Test
    void tamperedIvIsRejected() throws Exception {
        byte[] bad = sampleBlob();
        bad[KeyDeriver.SALT_LENGTH] ^= 1; // first byte of the IV
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));
    }

    @Test
    void tamperedCiphertextIsRejected() throws Exception {
        byte[] bad = sampleBlob();
        bad[KeyDeriver.SALT_LENGTH + 12] ^= 1; // first byte of the ciphertext
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));
    }

    @Test
    void tamperedTagIsRejected() throws Exception {
        byte[] bad = sampleBlob();
        bad[bad.length - 1] ^= 1; // last byte is part of the tag
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));
    }

    @Test
    void truncatedBlobIsRejected() throws Exception {
        byte[] blob = sampleBlob();
        byte[] bad = Arrays.copyOf(blob, blob.length - 1);
        assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));
    }

    @Test
    void wrongPasswordAndTamperingGiveTheSameMessage() throws Exception {
        byte[] blob = sampleBlob();
        byte[] bad = blob.clone();
        bad[bad.length - 1] ^= 1;

        Exception wrongPw = assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(blob, "wrong-pw".toCharArray()));
        Exception tampered = assertThrows(VaultDecryptionException.class,
                () -> crypto.decrypt(bad, password));

        assertEquals(wrongPw.getMessage(), tampered.getMessage());
    }
}