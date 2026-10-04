package com.harvey.vault.crypto;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Arrays;

public class CryptoService {
    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;
    private static final int TAG_BYTES = TAG_BITS / 8;
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private final KeyDeriver keyDeriver = new KeyDeriver();
    private final SecureRandom random = new SecureRandom();

    public byte[] encrypt(byte[] plain, char[] password) throws Exception {
        byte[] salt = keyDeriver.newSalt();
        byte[] iv = new byte[IV_LENGTH];
        random.nextBytes(iv);

        byte[] keyBytes = keyDeriver.derive(password, salt);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            byte[] ciphertext = cipher.doFinal(plain);

            return ByteBuffer.allocate(salt.length + iv.length + ciphertext.length)
                    .put(salt).put(iv).put(ciphertext)
                    .array();
        } finally {
            Arrays.fill(keyBytes, (byte) 0); // wipe the key copy
        }
    }

    public byte[] decrypt(byte[] blob, char[] password) throws Exception {
        int minLength = KeyDeriver.SALT_LENGTH + IV_LENGTH + TAG_BYTES;
        if (blob == null || blob.length < minLength) {
            throw new IllegalArgumentException("Data is too short to be a valid vault");
        }

        ByteBuffer buf = ByteBuffer.wrap(blob);
        byte[] salt = new byte[KeyDeriver.SALT_LENGTH];
        byte[] iv = new byte[IV_LENGTH];
        buf.get(salt);
        buf.get(iv);
        byte[] ciphertext = new byte[buf.remaining()];
        buf.get(ciphertext);

        byte[] keyBytes = keyDeriver.derive(password, salt);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE,
                    new SecretKeySpec(keyBytes, "AES"),
                    new GCMParameterSpec(TAG_BITS, iv));
            return cipher.doFinal(ciphertext);
        } finally {
            Arrays.fill(keyBytes, (byte) 0);
        }
    }
}