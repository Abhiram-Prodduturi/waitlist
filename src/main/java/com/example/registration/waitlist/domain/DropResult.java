package com.example.registration.waitlist.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Outcome of a drop request. Every outcome is explicit; none is signalled by an exception.
 */
public sealed interface DropResult {

    LearnerId learnerId();

    /**
     * The learner gave up their seat and promotion ran.
     *
     * @param learnerId                 the learner who dropped
     * @param promoted                  the learner moved from the waitlist into the seat, or {@code null}
     * @param removedIneligibleLearners learners removed from the head of the waitlist because they were
     *                                  no longer eligible (decision D-3), in the order they were removed
     */
    record Dropped(LearnerId learnerId, LearnerId promoted, List<LearnerId> removedIneligibleLearners)
            implements DropResult {

        public Dropped {
            Objects.requireNonNull(learnerId, "learnerId");
            removedIneligibleLearners = List.copyOf(removedIneligibleLearners);
        }

        public Optional<LearnerId> promotedLearner() {
            return Optional.ofNullable(promoted);
        }
    }

    /** The learner was waiting and left the waitlist; no seat opened, so nobody was promoted (decision D-7). */
    record LeftWaitlist(LearnerId learnerId) implements DropResult {

        public LeftWaitlist {
            Objects.requireNonNull(learnerId, "learnerId");
        }
    }

    /** No-op: the learner was neither enrolled nor waiting (decision D-8). */
    record NotEnrolled(LearnerId learnerId) implements DropResult {

        public NotEnrolled {
            Objects.requireNonNull(learnerId, "learnerId");
        }
    }
}
