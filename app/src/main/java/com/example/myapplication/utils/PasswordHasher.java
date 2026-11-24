package com.example.myapplication.utils;

import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordHasher {
    private static final int ITERATIONS = 600_000;
    private final SecureRandom random = new SecureRandom();

    public String hash(String password) {
        byte[] salt = new byte[16];
        random.nextBytes(salt);
        Base64.Encoder encoder = Base64.getEncoder();
        return ITERATIONS + ":" + encoder.encodeToString(salt) + ":"
                + encoder.encodeToString(derive(password, salt, ITERATIONS));
    }

    public boolean matches(String password, String encoded) {
        String[] parts = encoded.split(":");
        Base64.Decoder decoder = Base64.getDecoder();
        return MessageDigest.isEqual(decoder.decode(parts[2]),
                derive(password, decoder.decode(parts[1]), Integer.parseInt(parts[0])));
    }

    private byte[] derive(String password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), salt, iterations, 256);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("Proteksi kata sandi tidak tersedia.", failure);
        } finally {
            spec.clearPassword();
        }
    }
}
