package com.example.registration.waitlist.domain;

import java.util.List;
import java.util.Objects;

/**
 * Outcome of an enroll request. Every outcome is explicit; none is signalled by an exception.
 */
public sealed interface EnrollResult {

    LearnerId learnerId();

    /** The learner took a free seat. */
    record Enrolled(LearnerId learnerId) implements EnrollResult {

        public Enrolled {
            Objects.requireNonNull(learnerId, "learnerId");
        }
    }

    /** The offering was full; the learner joined the back of the waitlist at a 1-based position. */
    record Waitlisted(LearnerId learnerId, int position) implements EnrollResult {

        public Waitlisted {
            Objects.requireNonNull(learnerId, "learnerId");
            requirePositive(position);
        }
    }

    /** No-op: the learner already holds a seat (decision D-5). */
    record AlreadyEnrolled(LearnerId learnerId) implements EnrollResult {

        public AlreadyEnrolled {
            Objects.requireNonNull(learnerId, "learnerId");
        }
    }

    /** No-op: the learner is already waiting and keeps their 1-based position (decision D-6). */
    record AlreadyWaitlisted(LearnerId learnerId, int position) implements EnrollResult {

        public AlreadyWaitlisted {
            Objects.requireNonNull(learnerId, "learnerId");
            requirePositive(position);
        }
    }

    /** Rejected: the learner has not completed the listed prerequisites. State is unchanged. */
    record PrerequisitesNotMet(LearnerId learnerId, List<CourseCode> missingPrerequisites)
            implements EnrollResult {

        public PrerequisitesNotMet {
            Objects.requireNonNull(learnerId, "learnerId");
            missingPrerequisites = List.copyOf(missingPrerequisites);
            if (missingPrerequisites.isEmpty()) {
                throw new IllegalArgumentException("missingPrerequisites must not be empty");
            }
        }
    }

    private static void requirePositive(int position) {
        if (position < 1) {
            throw new IllegalArgumentException("position must be at least 1");
        }
    }
}
