package com.syncpoint.archive.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ProfileFieldsTest {

    @Test
    void acceptsTheFieldsTheProcedureCanApply() {
        assertDoesNotThrow(() -> ProfileFields.validate("city", "Johannesburg"));
        assertDoesNotThrow(() -> ProfileFields.validate("mobile_phone", "+27 82 000 0000"));
        assertDoesNotThrow(() -> ProfileFields.validate("email", "pat@example.com"));
        assertDoesNotThrow(() -> ProfileFields.validate("membership_number", "MA-12345"));
    }

    @Test
    void rejectsFieldsTheProcedureWouldIgnoreYetMarkApproved() {
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("id_number", "1234567890123"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("first_name", "X"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("country", "ZA"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate(null, "x"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("city; DROP TABLE users", "x"));
    }

    @Test
    void enforcesColumnLengths() {
        assertDoesNotThrow(() -> ProfileFields.validate("postal_code", "1".repeat(20)));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("postal_code", "1".repeat(21)));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("home_phone", "1".repeat(31)));
    }

    @Test
    void rejectsBlankControlCharactersAndBadEmails() {
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("city", "   "));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("city", null));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("city", "Cape\u0000Town"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("email", "not-an-email"));
        assertThrows(IllegalArgumentException.class, () -> ProfileFields.validate("secondary_email", "a@b"));
    }
}
