package com.example.registration.waitlist.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.registration.waitlist.adapter.out.memory.InMemoryLearnerDirectory;
import com.example.registration.waitlist.adapter.out.memory.InMemoryOfferingStore;
import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.DropResult;
import com.example.registration.waitlist.domain.EnrollResult;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.LearnerNotFoundException;
import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import com.example.registration.waitlist.domain.OfferingSnapshot;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Service behaviour against the real in-memory adapters: reference checks and re-reading learner data at
 * promotion time. Enrollment rules themselves are covered by {@code OfferingTest}.
 */
class EnrollmentServiceTest {

    private static final OfferingId OFFERING_ID = new OfferingId("CS201-2026F");
    private static final CourseCode CS101 = new CourseCode("CS101");
    private static final int CAPACITY = 2;

    private static final LearnerId ASHA = new LearnerId("L-001");
    private static final LearnerId BEN = new LearnerId("L-002");
    private static final LearnerId CHEN = new LearnerId("L-003");
    private static final LearnerId UNKNOWN = new LearnerId("L-999");

    private final InMemoryLearnerDirectory directory = new InMemoryLearnerDirectory();
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        InMemoryOfferingStore store = new InMemoryOfferingStore();
        store.add(new Offering(OFFERING_ID, CAPACITY, Set.of(CS101)));
        for (LearnerId learnerId : List.of(ASHA, BEN, CHEN)) {
            directory.put(new Learner(learnerId, Set.of(CS101)));
        }
        service = new EnrollmentService(store, directory);
    }

    @Test
    void testEnrollRejectsLearnerMissingFromDirectoryWithoutChangingState() {
        OfferingSnapshot before = service.view(OFFERING_ID);

        assertThatThrownBy(() -> service.enroll(OFFERING_ID, UNKNOWN))
                .isInstanceOf(LearnerNotFoundException.class)
                .hasMessageContaining("L-999");
        assertThat(service.view(OFFERING_ID)).isEqualTo(before);
    }

    @Test
    void testDropRejectsLearnerMissingFromDirectory() {
        assertThatThrownBy(() -> service.drop(OFFERING_ID, UNKNOWN))
                .isInstanceOf(LearnerNotFoundException.class)
                .hasMessageContaining("L-999");
    }

    @Test
    void testRejectsUnknownOffering() {
        OfferingId unknown = new OfferingId("XX999");

        assertThatThrownBy(() -> service.enroll(unknown, ASHA))
                .isInstanceOf(OfferingNotFoundException.class)
                .hasMessageContaining("XX999");
    }

    @Test
    void testPromotionReadsCurrentLearnerRecord() {
        service.enroll(OFFERING_ID, ASHA);
        service.enroll(OFFERING_ID, BEN);
        assertThat(service.enroll(OFFERING_ID, CHEN)).isEqualTo(new EnrollResult.Waitlisted(CHEN, 1));
        directory.put(new Learner(CHEN, Set.of()));

        DropResult result = service.drop(OFFERING_ID, ASHA);

        assertThat(result).isEqualTo(new DropResult.Dropped(ASHA, null, List.of(CHEN)));
        OfferingSnapshot snapshot = service.view(OFFERING_ID);
        assertThat(snapshot.enrolled()).containsExactly(BEN);
        assertThat(snapshot.waitlist()).isEmpty();
    }

    @Test
    void testDropOfWaitlistedLearnerLeavesWaitlist() {
        service.enroll(OFFERING_ID, ASHA);
        service.enroll(OFFERING_ID, BEN);
        service.enroll(OFFERING_ID, CHEN);

        assertThat(service.drop(OFFERING_ID, CHEN)).isEqualTo(new DropResult.LeftWaitlist(CHEN));
        assertThat(service.drop(OFFERING_ID, CHEN)).isEqualTo(new DropResult.NotEnrolled(CHEN));
        assertThat(service.view(OFFERING_ID).waitlist()).isEmpty();
    }
}
