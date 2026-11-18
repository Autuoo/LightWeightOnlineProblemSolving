package com.example.oj.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

@Component
public class SecretCipher {

    private static final int IV_LENGTH = 12;
    private static final int TAG_BITS = 128;

    private final SecretKeySpec key;
    private final SecureRandom random = new SecureRandom();

    public SecretCipher(@Value("${oj.ai.secret-key:dev-only-change-this-secret}") String secret) {
        this.key = new SecretKeySpec(sha256(secret), "AES");
    }

    public String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return "";
        }
        try {
            byte[] iv = new byte[IV_LENGTH];
            random.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            byte[] combined = new byte[iv.length + encrypted.length];
            System.arraycopy(iv, 0, combined, 0, iv.length);
            System.arraycopy(encrypted, 0, combined, iv.length, encrypted.length);
            return Base64.getEncoder().encodeToString(combined);
        } catch (Exception ex) {
            throw new IllegalStateException("API Key 加密失败", ex);
        }
    }

    public String decrypt(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        try {
            byte[] combined = Base64.getDecoder().decode(value);
            if (combined.length <= IV_LENGTH) {
                return "";
            }
            byte[] iv = Arrays.copyOfRange(combined, 0, IV_LENGTH);
            byte[] data = Arrays.copyOfRange(combined, IV_LENGTH, combined.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception ex) {
            return "";
        }
    }

    private static byte[] sha256(String secret) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("密钥派生失败", ex);
        }
    }
}