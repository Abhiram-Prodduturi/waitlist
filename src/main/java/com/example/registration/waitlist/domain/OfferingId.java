package com.example.registration.waitlist.domain;

/**
 * Identifier of a course offering.
 *
 * @param value letters, digits, and hyphens, 1 to 64 characters
 */
public record OfferingId(String value) {

    public OfferingId {
        Identifiers.requireValid(value, "offeringId");
    }

    @Override
    public String toString() {
        return value;
    }
}
