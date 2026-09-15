package com.nexora.sport.util;

import java.security.SecureRandom;

public final class PasswordGenerator {
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789";
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordGenerator() {}

    public static String generate() {
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {
            sb.append(CHARS.charAt(RANDOM.nextInt(CHARS.length())));
        }
        return sb.toString();
    }

    public static String generateToken() {
        StringBuilder sb = new StringBuilder(48);
        String tokenChars = CHARS + "0123456789";
        for (int i = 0; i < 48; i++) {
            sb.append(tokenChars.charAt(RANDOM.nextInt(tokenChars.length())));
        }
        return sb.toString();
    }
}
