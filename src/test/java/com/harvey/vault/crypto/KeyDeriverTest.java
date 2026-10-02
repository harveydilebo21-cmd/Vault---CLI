package com.harvey.vault.crypto;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class KeyDeriverTest {

    private final KeyDeriver deriver = new KeyDeriver();

    @Test
    void sameInputsProduceSameKey() throws Exception {
        byte[] salt = new byte[16];
        byte[] k1 = deriver.derive("pw".toCharArray(), salt);
        byte[] k2 = deriver.derive("pw".toCharArray(), salt);
        assertArrayEquals(k1, k2);
    }

    @Test
    void differentPasswordsProduceDifferentKeys() throws Exception {
        byte[] salt = new byte[16];
        byte[] k1 = deriver.derive("pw1".toCharArray(), salt);
        byte[] k2 = deriver.derive("pw2".toCharArray(), salt);
        assertFalse(java.util.Arrays.equals(k1, k2));
    }

    @Test
    void differentSaltsProduceDifferentKeys() throws Exception {
        byte[] s1 = new byte[16];
        byte[] s2 = new byte[16];
        s2[0] = 1;
        byte[] k1 = deriver.derive("pw".toCharArray(), s1);
        byte[] k2 = deriver.derive("pw".toCharArray(), s2);
        assertFalse(java.util.Arrays.equals(k1, k2));
    }

    @Test
    void keyIs256Bits() throws Exception {
        byte[] key = deriver.derive("pw".toCharArray(), new byte[16]);
        assertEquals(32, key.length);
    }

    @Test
    void generatedSaltsAreRandomAndCorrectLength() {
        byte[] a = deriver.newSalt();
        byte[] b = deriver.newSalt();
        assertEquals(16, a.length);
        assertFalse(java.util.Arrays.equals(a, b));
    }
}