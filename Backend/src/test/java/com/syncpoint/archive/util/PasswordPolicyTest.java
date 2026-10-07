package com.syncpoint.archive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordPolicyTest {

    @Test
    void acceptsNormalPasswords() {
        assertDoesNotThrow(() -> PasswordPolicy.validate("a-reasonable-password"));
    }

    @Test
    void rejectsBlank() {
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate("  "));
        assertThrows(IllegalArgumentException.class, () -> PasswordPolicy.validate(null));
    }

    @Test
    void limitIsMeasuredInBytesNotCharacters() {
        assertTrue(PasswordPolicy.fitsBcrypt("a".repeat(72)));
        assertFalse(PasswordPolicy.fitsBcrypt("a".repeat(73)));
        // 37 two-byte characters = 74 bytes
        assertFalse(PasswordPolicy.fitsBcrypt("é".repeat(37)));
    }
}
