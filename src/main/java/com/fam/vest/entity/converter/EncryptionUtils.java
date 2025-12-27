package com.fam.vest.entity.converter;

import org.springframework.stereotype.Component;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

@Component
public final class EncryptionUtils {

    private static final String ALGORITHM = "AES";
    private static SecretKeySpec secretKeySpec;

    private EncryptionUtils() {
        // Prevent instantiation
    }

    public static void initializeSecretKey(String secretKey) {
        secretKeySpec = new SecretKeySpec(secretKey.getBytes(), ALGORITHM);
    }

    public static String encrypt(String value) {
        if (secretKeySpec == null) {
            throw new IllegalStateException("Encryption key not initialized");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKeySpec);
            return Base64.getEncoder().encodeToString(cipher.doFinal(value.getBytes()));
        } catch (Exception e) {
            throw new RuntimeException("Error encrypting", e);
        }
    }

    public static String decrypt(String encryptedValue) {
        if (secretKeySpec == null) {
            throw new IllegalStateException("Encryption key not initialized");
        }
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedValue);
            return new String(cipher.doFinal(decoded));
        } catch (Exception e) {
            throw new RuntimeException("Error decrypting", e);
        }
    }
}
