package com.scentzone.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private static final int ITERATIONS = 210_000;
    private static final int KEY_LENGTH = 256;
    private static final SecureRandom RNG = new SecureRandom();

    private PasswordUtil() {}

    public static String hash(String password) {
        if (password == null || password.length() < 8)
            throw new IllegalArgumentException("Password must contain at least 8 characters.");
        byte[] salt = new byte[16];
        RNG.nextBytes(salt);
        return "pbkdf2:" + ITERATIONS + ":" + Base64.getEncoder().encodeToString(salt)
                + ":" + Base64.getEncoder().encodeToString(derive(password, salt, ITERATIONS));
    }

    public static boolean verify(String password, String encoded) {
        if (password == null || encoded == null) return false;
        try {
            String[] parts = encoded.split(":", -1);
            if (parts.length != 4 || !"pbkdf2".equals(parts[0])) return false;
            int rounds = Integer.parseInt(parts[1]);
            if (rounds < 100_000 || rounds > 2_000_000) return false;
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] actual = Base64.getDecoder().decode(parts[3]);
            if (salt.length != 16 || actual.length != KEY_LENGTH / 8) return false;
            return MessageDigest.isEqual(actual, derive(password, salt, rounds));
        } catch (RuntimeException ex) {
            return false;
        }
    }

    private static byte[] derive(String password, byte[] salt, int iterations) {
        char[] chars = password.toCharArray();
        PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_LENGTH);
        try {
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("Password hashing unavailable", e);
        } finally {
            spec.clearPassword();
            java.util.Arrays.fill(chars, '\0');
        }
    }
}
