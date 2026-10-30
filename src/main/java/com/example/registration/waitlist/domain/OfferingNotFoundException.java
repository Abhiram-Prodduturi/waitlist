package com.example.registration.waitlist.domain;

/**
 * The offering ID is well formed but no such offering exists.
 */
public final class OfferingNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient OfferingId offeringId;

    public OfferingNotFoundException(OfferingId offeringId) {
        super("Offering " + offeringId + " was not found");
        this.offeringId = offeringId;
    }

    public OfferingId offeringId() {
        return offeringId;
    }
}
