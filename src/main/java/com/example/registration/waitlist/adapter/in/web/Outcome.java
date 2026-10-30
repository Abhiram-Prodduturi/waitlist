package com.example.registration.waitlist.adapter.in.web;

/**
 * Outcome codes returned in every response body (DESIGN.md section 3).
 */
public enum Outcome {
    ENROLLED,
    WAITLISTED,
    ALREADY_ENROLLED,
    ALREADY_WAITLISTED,
    PREREQUISITES_NOT_MET,
    DROPPED,
    LEFT_WAITLIST,
    NOT_ENROLLED,
    INVALID_REQUEST,
    LEARNER_NOT_FOUND,
    OFFERING_NOT_FOUND
}
