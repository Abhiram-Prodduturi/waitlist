package com.example.registration.waitlist.domain;

/**
 * The learner ID is well formed but not in the learner directory.
 */
public final class LearnerNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final transient LearnerId learnerId;

    public LearnerNotFoundException(LearnerId learnerId) {
        super("Learner " + learnerId + " was not found");
        this.learnerId = learnerId;
    }

    public LearnerId learnerId() {
        return learnerId;
    }
}
