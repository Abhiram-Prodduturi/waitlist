package com.example.registration.waitlist.domain;

/**
 * Identifier of a learner.
 *
 * @param value letters, digits, and hyphens, 1 to 64 characters
 */
public record LearnerId(String value) {

    public LearnerId {
        Identifiers.requireValid(value, "learnerId");
    }

    @Override
    public String toString() {
        return value;
    }
}
