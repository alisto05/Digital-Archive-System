package com.syncpoint.archive.util;

import java.nio.charset.StandardCharsets;


public final class PasswordPolicy {

    public static final int MAX_BYTES = 72;

    private PasswordPolicy() {
    }

    public static boolean fitsBcrypt(String password) {
        return password != null && password.getBytes(StandardCharsets.UTF_8).length <= MAX_BYTES;
    }

   
    public static void validate(String password) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("Password is required.");
        }
        if (!fitsBcrypt(password)) {
            throw new IllegalArgumentException("Password must not exceed 72 bytes.");
        }
    }
}
