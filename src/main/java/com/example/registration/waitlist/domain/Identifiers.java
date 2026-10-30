package com.example.registration.waitlist.domain;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Shared validation for identifier value objects.
 */
public final class Identifiers {

    /** Letters, digits, and hyphens, 1 to 64 characters. Also used for Bean Validation at the boundaries. */
    public static final String PATTERN_TEXT = "^[A-Za-z0-9-]{1,64}$";

    private static final Pattern PATTERN = Pattern.compile(PATTERN_TEXT);

    private Identifiers() {
        // Static helpers only
    }

    static String requireValid(String value, String name) {
        Objects.requireNonNull(value, name);
        if (!PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " must match " + PATTERN_TEXT);
        }
        return value;
    }
}
