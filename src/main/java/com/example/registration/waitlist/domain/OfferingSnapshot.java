package com.example.registration.waitlist.domain;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable copy of an offering's state at one point in time.
 *
 * @param id            offering identifier
 * @param capacity      total seats
 * @param prerequisites courses a learner must have completed
 * @param enrolled      enrolled learners in the order they took a seat
 * @param waitlist      waiting learners in FIFO order; index 0 is position 1
 */
public record OfferingSnapshot(
        OfferingId id,
        int capacity,
        Set<CourseCode> prerequisites,
        List<LearnerId> enrolled,
        List<LearnerId> waitlist) {

    public OfferingSnapshot {
        Objects.requireNonNull(id, "id");
        prerequisites = Set.copyOf(prerequisites);
        enrolled = List.copyOf(enrolled);
        waitlist = List.copyOf(waitlist);
    }

    public int seatsAvailable() {
        return capacity - enrolled.size();
    }
}
