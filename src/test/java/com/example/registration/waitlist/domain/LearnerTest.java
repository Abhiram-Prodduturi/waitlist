package com.example.registration.waitlist.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class LearnerTest {

    private static final LearnerId LEARNER_ID = new LearnerId("L-001");
    private static final CourseCode CS101 = new CourseCode("CS101");
    private static final CourseCode MA101 = new CourseCode("MA101");
    private static final CourseCode PH101 = new CourseCode("PH101");

    @Test
    void testReportsMissingPrerequisitesSortedByCode() {
        Learner learner = new Learner(LEARNER_ID, Set.of(MA101));

        assertThat(learner.missingPrerequisites(Set.of(PH101, CS101, MA101))).containsExactly(CS101, PH101);
        assertThat(learner.hasCompletedAll(Set.of(PH101, CS101, MA101))).isFalse();
    }

    @Test
    void testIsEligibleWhenEveryPrerequisiteIsCompleted() {
        Learner learner = new Learner(LEARNER_ID, Set.of(CS101, MA101));

        assertThat(learner.missingPrerequisites(List.of(CS101))).isEmpty();
        assertThat(learner.hasCompletedAll(List.of(CS101))).isTrue();
    }

    @Test
    void testCopiesCompletedCoursesDefensively() {
        Set<CourseCode> completed = new HashSet<>(Set.of(CS101));
        Learner learner = new Learner(LEARNER_ID, completed);

        completed.clear();

        assertThat(learner.completedCourses()).containsExactly(CS101);
    }
}
