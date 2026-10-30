package com.example.registration.waitlist.domain;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.SequencedSet;
import java.util.Set;
import java.util.function.Function;

/**
 * Enrollment state and rules for one course offering.
 *
 * <p>Invariants after every public method (see {@code docs/DESIGN.md} section 6):
 * <ul>
 *   <li>I-1: enrolled count never exceeds capacity;</li>
 *   <li>I-2: a learner is in at most one of roster and waitlist, at most once;</li>
 *   <li>I-3: a free seat implies an empty waitlist;</li>
 *   <li>I-4: the waitlist keeps join order.</li>
 * </ul>
 *
 * <p>Not thread-safe. Callers must serialize access, for example by holding a per-offering lock.
 */
public final class Offering {

    private final OfferingId id;
    private final int capacity;
    private final Set<CourseCode> prerequisites;
    private final SequencedSet<LearnerId> enrolled = new LinkedHashSet<>();
    private final SequencedSet<LearnerId> waitlist = new LinkedHashSet<>();

    public Offering(OfferingId id, int capacity, Set<CourseCode> prerequisites) {
        this.id = Objects.requireNonNull(id, "id");
        if (capacity < 1) {
            throw new IllegalArgumentException("capacity must be at least 1");
        }
        this.capacity = capacity;
        this.prerequisites = Set.copyOf(Objects.requireNonNull(prerequisites, "prerequisites"));
    }

    public OfferingId id() {
        return id;
    }

    /**
     * Applies an enroll request. Checks run in a fixed order and the first match decides:
     * already enrolled, already waitlisted, prerequisites, then seat availability.
     * Duplicate and rejected requests leave the state unchanged.
     */
    public EnrollResult enroll(Learner learner) {
        Objects.requireNonNull(learner, "learner");
        LearnerId learnerId = learner.id();
        return existingRecord(learnerId)
                .or(() -> prerequisiteFailure(learner))
                .orElseGet(() -> admit(learnerId));
    }

    /**
     * Applies a drop request. Dropping an enrolled learner frees a seat and runs promotion; dropping a
     * waiting learner removes them from the waitlist; any other learner is a no-op.
     *
     * @param learnerId     the learner who wants to drop
     * @param learnerLookup current learner data, used to re-check eligibility during promotion (decision D-2).
     *                      A learner it cannot find is treated as ineligible (assumption A-2).
     */
    public DropResult drop(LearnerId learnerId, Function<LearnerId, Optional<Learner>> learnerLookup) {
        Objects.requireNonNull(learnerId, "learnerId");
        Objects.requireNonNull(learnerLookup, "learnerLookup");
        DropResult result;
        if (enrolled.remove(learnerId)) {
            result = promoteInto(learnerId, learnerLookup);
        } else if (waitlist.remove(learnerId)) {
            result = new DropResult.LeftWaitlist(learnerId);
        } else {
            result = new DropResult.NotEnrolled(learnerId);
        }
        return result;
    }

    public OfferingSnapshot snapshot() {
        return new OfferingSnapshot(id, capacity, prerequisites, List.copyOf(enrolled), List.copyOf(waitlist));
    }

    private Optional<EnrollResult> existingRecord(LearnerId learnerId) {
        Optional<EnrollResult> existing;
        if (enrolled.contains(learnerId)) {
            existing = Optional.of(new EnrollResult.AlreadyEnrolled(learnerId));
        } else if (waitlist.contains(learnerId)) {
            existing = Optional.of(new EnrollResult.AlreadyWaitlisted(learnerId, positionOf(learnerId)));
        } else {
            existing = Optional.empty();
        }
        return existing;
    }

    private Optional<EnrollResult> prerequisiteFailure(Learner learner) {
        List<CourseCode> missing = learner.missingPrerequisites(prerequisites);
        return missing.isEmpty()
                ? Optional.empty()
                : Optional.of(new EnrollResult.PrerequisitesNotMet(learner.id(), missing));
    }

    /**
     * Seats the learner if one is free, otherwise appends them to the waitlist. By invariant I-3 a free seat
     * means nobody is waiting, so taking it directly never jumps the queue.
     */
    private EnrollResult admit(LearnerId learnerId) {
        if (hasFreeSeat()) {
            enrolled.add(learnerId);
            return new EnrollResult.Enrolled(learnerId);
        }
        waitlist.add(learnerId);
        return new EnrollResult.Waitlisted(learnerId, waitlist.size());
    }

    /**
     * Fills free seats from the head of the waitlist. An ineligible head is removed and the scan continues
     * (decision D-3). Eligibility is evaluated before the head is removed, so a failing lookup leaves that
     * learner in place.
     */
    private DropResult.Dropped promoteInto(LearnerId dropped, Function<LearnerId, Optional<Learner>> lookup) {
        LearnerId promoted = null;
        List<LearnerId> removedIneligible = new ArrayList<>();
        while (hasFreeSeat() && !waitlist.isEmpty()) {
            LearnerId head = waitlist.getFirst();
            boolean eligible = lookup.apply(head)
                    .map(learner -> learner.hasCompletedAll(prerequisites))
                    .orElse(false);
            waitlist.removeFirst();
            if (eligible) {
                enrolled.add(head);
                promoted = head;
            } else {
                removedIneligible.add(head);
            }
        }
        return new DropResult.Dropped(dropped, promoted, removedIneligible);
    }

    private boolean hasFreeSeat() {
        return enrolled.size() < capacity;
    }

    private int positionOf(LearnerId learnerId) {
        int position = 1;
        for (LearnerId waiting : waitlist) {
            if (waiting.equals(learnerId)) {
                break;
            }
            position++;
        }
        return position;
    }
}
