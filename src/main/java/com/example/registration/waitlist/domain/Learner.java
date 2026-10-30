package com.example.registration.waitlist.domain;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * A learner and the courses they have completed.
 *
 * @param id               learner identifier
 * @param completedCourses courses the learner has completed; copied defensively
 */
public record Learner(LearnerId id, Set<CourseCode> completedCourses) {

    public Learner {
        Objects.requireNonNull(id, "id");
        completedCourses = Set.copyOf(Objects.requireNonNull(completedCourses, "completedCourses"));
    }

    /**
     * Returns the prerequisites this learner has not completed, sorted by code.
     * An empty list means the learner is eligible (decision D-1).
     */
    public List<CourseCode> missingPrerequisites(Collection<CourseCode> prerequisites) {
        return prerequisites.stream()
                .filter(course -> !completedCourses.contains(course))
                .sorted()
                .toList();
    }

    public boolean hasCompletedAll(Collection<CourseCode> prerequisites) {
        return completedCourses.containsAll(prerequisites);
    }
}
