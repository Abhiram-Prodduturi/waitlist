package com.example.registration.waitlist.adapter.out.memory;

import com.example.registration.waitlist.application.port.out.LearnerDirectory;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory stand-in for the learner records system. Thread-safe.
 *
 * <p>{@link #put} adds or replaces a learner. It is used to seed learners at startup and by tests to change a
 * learner's completed courses while they wait (assumption A-1).
 */
public final class InMemoryLearnerDirectory implements LearnerDirectory {

    private final Map<LearnerId, Learner> learners = new ConcurrentHashMap<>();

    public void put(Learner learner) {
        Objects.requireNonNull(learner, "learner");
        learners.put(learner.id(), learner);
    }

    @Override
    public Optional<Learner> find(LearnerId learnerId) {
        return Optional.ofNullable(learners.get(Objects.requireNonNull(learnerId, "learnerId")));
    }
}
