package com.syncpoint.archive.util;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * The profile fields sp_resolve_change_request knows how to apply, with the column
 * lengths from the schema. The procedure silently ignores unknown fields yet still marks
 * the request APPROVED, and a too-long value would fail mid-transaction, so both are
 * rejected here when the request is submitted.
 */
public final class ProfileFields {

    private static final Map<String, Integer> MAX_LENGTH = Map.ofEntries(
            Map.entry("address_line", 255),
            Map.entry("city", 100),
            Map.entry("postal_code", 20),
            Map.entry("province", 100),
            Map.entry("home_phone", 30),
            Map.entry("work_phone", 30),
            Map.entry("mobile_phone", 30),
            Map.entry("secondary_phone", 30),
            Map.entry("email", 255),
            Map.entry("secondary_email", 255),
            Map.entry("provider", 150),
            Map.entry("membership_number", 100)
    );

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private ProfileFields() {
    }

    /** @throws IllegalArgumentException with a client-safe message */
    public static void validate(String field, String value) {
        Integer max = field == null ? null : MAX_LENGTH.get(field);
        if (max == null) {
            throw new IllegalArgumentException("That field cannot be changed through a request.");
        }
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("A new value is required.");
        }
        if (value.length() > max) {
            throw new IllegalArgumentException("The new value must not exceed " + max + " characters.");
        }
        if (value.chars().anyMatch(Character::isISOControl)) {
            throw new IllegalArgumentException("The new value contains invalid characters.");
        }
        if (field.endsWith("email") && !EMAIL.matcher(value).matches()) {
            throw new IllegalArgumentException("Enter a valid email address.");
        }
    }
}
