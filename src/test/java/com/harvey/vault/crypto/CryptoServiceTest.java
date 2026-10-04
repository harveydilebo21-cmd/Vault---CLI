package com.harvey.vault.crypto;

import org.junit.jupiter.api.Test;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;

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
}