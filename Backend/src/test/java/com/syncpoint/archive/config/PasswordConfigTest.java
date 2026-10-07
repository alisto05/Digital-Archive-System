package com.syncpoint.archive.config;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

import static org.junit.jupiter.api.Assertions.*;

class PasswordConfigTest {

    private final PasswordEncoder encoder = new PasswordConfig().passwordEncoder();

    private static String legacyHash(String salt, String password) throws Exception {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest((salt + password).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : digest) {
            hex.append(String.format("%02x", b));
        }
        return salt + "$" + hex;
    }

    @Test
    void newPasswordsAreBcryptAndVerify() {
        String hash = encoder.encode("correct horse");
        assertTrue(PasswordConfig.isBCryptHash(hash));
        assertTrue(encoder.matches("correct horse", hash));
        assertFalse(encoder.matches("wrong", hash));
    }

    @Test
    void legacySaltSha256HashesStillVerify() throws Exception {
        String stored = legacyHash("00112233445566778899aabbccddeeff", "s3cret");
        assertFalse(PasswordConfig.isBCryptHash(stored));
        assertTrue(encoder.matches("s3cret", stored));
        assertFalse(encoder.matches("S3cret", stored));
    }

    @Test
    void garbageHashesNeverMatch() {
        assertFalse(encoder.matches("x", null));
        assertFalse(encoder.matches("x", ""));
        assertFalse(encoder.matches("x", "not-a-hash"));
    }
}
