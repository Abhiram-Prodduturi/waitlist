package com.example.registration.waitlist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ResultTypesTest {

    private static final LearnerId ASHA = new LearnerId("L-001");
    private static final LearnerId BEN = new LearnerId("L-002");

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void testRejectsWaitlistPositionBelowOne(int position) {
        assertThatThrownBy(() -> new EnrollResult.Waitlisted(ASHA, position))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position");
    }

    @Test
    void testRejectsAlreadyWaitlistedPositionBelowOne() {
        assertThatThrownBy(() -> new EnrollResult.AlreadyWaitlisted(ASHA, 0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("position");
    }

    @Test
    void testRejectsPrerequisiteFailureWithoutMissingCourses() {
        List<CourseCode> none = List.of();

        assertThatThrownBy(() -> new EnrollResult.PrerequisitesNotMet(ASHA, none))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missingPrerequisites");
    }

    @Test
    void testDroppedExposesPromotedLearnerAndCopiesRemovedList() {
        List<LearnerId> removed = new ArrayList<>(List.of(BEN));
        DropResult.Dropped dropped = new DropResult.Dropped(ASHA, BEN, removed);

        removed.clear();

        assertThat(dropped.promotedLearner()).contains(BEN);
        assertThat(dropped.removedIneligibleLearners()).containsExactly(BEN);
    }

    @Test
    void testSnapshotReportsSeatsAvailable() {
        OfferingSnapshot snapshot = new OfferingSnapshot(
                new OfferingId("CS201-2026F"), 3, Set.of(), List.of(ASHA), List.of());

        assertThat(snapshot.seatsAvailable()).isEqualTo(2);
    }

    @Test
    void testNotFoundExceptionsCarryTheIdentifier() {
        OfferingId offeringId = new OfferingId("CS201-2026F");

        assertThat(new LearnerNotFoundException(ASHA).learnerId()).isEqualTo(ASHA);
        assertThat(new OfferingNotFoundException(offeringId))
                .hasMessageContaining("CS201-2026F")
                .extracting(OfferingNotFoundException::offeringId)
                .isEqualTo(offeringId);
    }
}
