package com.example.registration.waitlist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.registration.waitlist.domain.DropResult.Dropped;
import com.example.registration.waitlist.domain.DropResult.LeftWaitlist;
import com.example.registration.waitlist.domain.DropResult.NotEnrolled;
import com.example.registration.waitlist.domain.EnrollResult.AlreadyEnrolled;
import com.example.registration.waitlist.domain.EnrollResult.AlreadyWaitlisted;
import com.example.registration.waitlist.domain.EnrollResult.Enrolled;
import com.example.registration.waitlist.domain.EnrollResult.PrerequisitesNotMet;
import com.example.registration.waitlist.domain.EnrollResult.Waitlisted;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Rules for enroll, drop, and promotion on one offering. Invariants I-1 to I-4 are checked after every test,
 * and at intermediate steps where a test changes state more than once. Fixture: 2 seats, prerequisite CS101.
 */
class OfferingTest {

    private static final OfferingId OFFERING_ID = new OfferingId("CS201-2026F");
    private static final CourseCode CS101 = new CourseCode("CS101");
    private static final CourseCode MA101 = new CourseCode("MA101");
    private static final int CAPACITY = 2;

    private static final LearnerId ASHA = new LearnerId("L-001");
    private static final LearnerId BEN = new LearnerId("L-002");
    private static final LearnerId CHEN = new LearnerId("L-003");
    private static final LearnerId DEV = new LearnerId("L-004");
    private static final LearnerId ELA = new LearnerId("L-005");
    private static final LearnerId FARID = new LearnerId("L-006");

    private final Map<LearnerId, Learner> directory = new HashMap<>();
    private Offering offering;

    @BeforeEach
    void setUp() {
        offering = new Offering(OFFERING_ID, CAPACITY, Set.of(CS101));
        for (LearnerId eligible : List.of(ASHA, BEN, CHEN, DEV, ELA)) {
            grantPrerequisite(eligible);
        }
        revokePrerequisite(FARID);
    }

    @AfterEach
    void checkInvariants() {
        assertInvariants();
    }

    // Available capacity

    @Test
    @DisplayName("An eligible learner takes a free seat")
    void testEnrollsEligibleLearnerWhenSeatIsFree() {
        EnrollResult result = enroll(ASHA);

        assertThat(result).isEqualTo(new Enrolled(ASHA));
        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).containsExactly(ASHA);
        assertThat(snapshot.seatsAvailable()).isEqualTo(1);
        assertThat(offering.id()).isEqualTo(OFFERING_ID);
    }

    // Full capacity

    @Test
    @DisplayName("When full, eligible learners join the back of the waitlist in request order")
    void testWaitlistsEligibleLearnersInJoinOrderWhenFull() {
        fillSeats();

        assertThat(enroll(CHEN)).isEqualTo(new Waitlisted(CHEN, 1));
        assertThat(enroll(DEV)).isEqualTo(new Waitlisted(DEV, 2));

        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).containsExactly(ASHA, BEN);
        assertThat(snapshot.waitlist()).containsExactly(CHEN, DEV);
    }

    // Prerequisite failure

    @ParameterizedTest(name = "seats already taken = {0}")
    @ValueSource(ints = {0, CAPACITY})
    @DisplayName("A learner without the prerequisite is neither enrolled nor waitlisted")
    void testRejectsLearnerWithoutPrerequisite(int seatsTaken) {
        List.of(ASHA, BEN).stream().limit(seatsTaken).forEach(this::enroll);
        OfferingSnapshot before = offering.snapshot();

        EnrollResult result = enroll(FARID);

        assertThat(result).isEqualTo(new PrerequisitesNotMet(FARID, List.of(CS101)));
        assertThat(offering.snapshot()).as("state after rejected request").isEqualTo(before);
    }

    @Test
    void testReportsEveryMissingPrerequisiteInCodeOrder() {
        Offering advanced = new Offering(OFFERING_ID, CAPACITY, Set.of(MA101, CS101));

        EnrollResult result = advanced.enroll(directory.get(FARID));

        assertThat(result).isEqualTo(new PrerequisitesNotMet(FARID, List.of(CS101, MA101)));
        assertThat(advanced.snapshot().enrolled()).isEmpty();
    }

    // Duplicate requests

    @Test
    @DisplayName("Enrolling again while enrolled is a no-op")
    void testRepeatedEnrollWhileEnrolledIsNoOp() {
        enroll(ASHA);
        OfferingSnapshot before = offering.snapshot();

        assertThat(enroll(ASHA)).isEqualTo(new AlreadyEnrolled(ASHA));
        assertThat(offering.snapshot()).isEqualTo(before);
    }

    @Test
    @DisplayName("Enrolling again while waitlisted is a no-op and keeps the learner's position")
    void testRepeatedEnrollWhileWaitlistedKeepsPosition() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        OfferingSnapshot before = offering.snapshot();

        assertThat(enroll(DEV)).isEqualTo(new AlreadyWaitlisted(DEV, 2));
        assertThat(enroll(CHEN)).isEqualTo(new AlreadyWaitlisted(CHEN, 1));
        assertThat(offering.snapshot()).isEqualTo(before);
    }

    @Test
    @DisplayName("Duplicate checks run before the prerequisite check")
    void testDuplicateCheckRunsBeforePrerequisiteCheck() {
        enroll(ASHA);
        revokePrerequisite(ASHA);

        assertThat(enroll(ASHA)).isEqualTo(new AlreadyEnrolled(ASHA));
        assertThat(offering.snapshot().enrolled()).containsExactly(ASHA);
    }

    // FIFO promotion

    @Test
    @DisplayName("Each drop promotes the earliest waitlisted learner")
    void testDropPromotesWaitlistedLearnersInFifoOrder() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        enroll(ELA);

        assertThat(drop(ASHA)).isEqualTo(new Dropped(ASHA, CHEN, List.of()));
        assertInvariants();
        assertThat(drop(BEN)).isEqualTo(new Dropped(BEN, DEV, List.of()));

        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).containsExactly(CHEN, DEV);
        assertThat(snapshot.waitlist()).containsExactly(ELA);
    }

    @Test
    void testDropWithEmptyWaitlistFreesSeat() {
        fillSeats();

        DropResult result = drop(ASHA);

        assertThat(result).isEqualTo(new Dropped(ASHA, null, List.of()));
        assertThat(((Dropped) result).promotedLearner()).isEmpty();
        assertThat(offering.snapshot().seatsAvailable()).isEqualTo(1);
    }

    // Ineligible learner at the head of the waitlist (decision D-3: skip and remove)

    @Test
    @DisplayName("An ineligible head is removed and the next eligible learner is promoted")
    void testDropRemovesIneligibleHeadAndPromotesNext() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        revokePrerequisite(CHEN);

        DropResult result = drop(ASHA);

        assertThat(result).isEqualTo(new Dropped(ASHA, DEV, List.of(CHEN)));
        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).containsExactly(BEN, DEV).doesNotContain(CHEN);
        assertThat(snapshot.waitlist()).isEmpty();
    }

    @Test
    @DisplayName("A waitlisted learner missing from the directory is treated as ineligible")
    void testDropTreatsLearnerMissingFromDirectoryAsIneligible() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        directory.remove(CHEN);

        assertThat(drop(ASHA)).isEqualTo(new Dropped(ASHA, DEV, List.of(CHEN)));
    }

    @Test
    @DisplayName("If every waiting learner is ineligible, all are removed and the seat stays free")
    void testDropWithOnlyIneligibleWaitersLeavesSeatFree() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        revokePrerequisite(CHEN);
        revokePrerequisite(DEV);

        assertThat(drop(ASHA)).isEqualTo(new Dropped(ASHA, null, List.of(CHEN, DEV)));
        assertInvariants();
        assertThat(offering.snapshot().seatsAvailable()).isEqualTo(1);
        assertThat(enroll(ELA)).as("next eligible request takes the free seat").isEqualTo(new Enrolled(ELA));
    }

    @Test
    @DisplayName("A removed learner who becomes eligible again rejoins at the back")
    void testRemovedIneligibleLearnerRejoinsAtBack() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        enroll(ELA);
        revokePrerequisite(CHEN);
        drop(ASHA);
        grantPrerequisite(CHEN);

        assertThat(enroll(CHEN)).isEqualTo(new Waitlisted(CHEN, 2));
        assertThat(offering.snapshot().waitlist()).containsExactly(ELA, CHEN);
    }

    // Leaving the waitlist (decision D-7)

    @Test
    @DisplayName("A waitlisted learner who drops leaves the waitlist and nobody is promoted")
    void testWaitlistedLearnerLeavesWithoutPromotion() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        enroll(ELA);

        assertThat(drop(DEV)).isEqualTo(new LeftWaitlist(DEV));

        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).containsExactly(ASHA, BEN);
        assertThat(snapshot.waitlist()).containsExactly(CHEN, ELA);
    }

    // Invalid or repeated drops (decision D-8)

    @Test
    @DisplayName("A repeated drop is a no-op and never frees a second seat")
    void testRepeatedDropIsNoOpAndPromotesNobody() {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        drop(ASHA);
        OfferingSnapshot before = offering.snapshot();

        assertThat(drop(ASHA)).isEqualTo(new NotEnrolled(ASHA));
        assertThat(offering.snapshot()).as("DEV must still be waiting").isEqualTo(before);
        assertThat(before.waitlist()).containsExactly(DEV);
    }

    @Test
    void testDropFromWaitlistTwiceIsNoOpTheSecondTime() {
        fillSeats();
        enroll(CHEN);
        drop(CHEN);
        OfferingSnapshot before = offering.snapshot();

        assertThat(drop(CHEN)).isEqualTo(new NotEnrolled(CHEN));
        assertThat(offering.snapshot()).isEqualTo(before);
    }

    @Test
    void testDropOfLearnerWithNoRecordIsNoOp() {
        fillSeats();
        enroll(CHEN);
        OfferingSnapshot before = offering.snapshot();

        assertThat(drop(ELA)).isEqualTo(new NotEnrolled(ELA));
        assertThat(offering.snapshot()).isEqualTo(before);
    }

    // Re-enrolling after a drop (decision D-4)

    @Test
    @DisplayName("A learner who dropped can request again and is treated as a new request")
    void testReenrollAfterDropIsTreatedAsNewRequest() {
        fillSeats();
        enroll(CHEN);
        drop(ASHA);

        assertThat(enroll(ASHA)).as("offering is full again after CHEN's promotion")
                .isEqualTo(new Waitlisted(ASHA, 1));
    }

    @Test
    void testReenrollAfterDropTakesFreeSeat() {
        enroll(ASHA);
        drop(ASHA);

        assertThat(enroll(ASHA)).isEqualTo(new Enrolled(ASHA));
    }

    // Construction

    @ParameterizedTest
    @ValueSource(ints = {0, -1})
    void testRejectsCapacityBelowOne(int capacity) {
        Set<CourseCode> prerequisites = Set.of(CS101);

        assertThatThrownBy(() -> new Offering(OFFERING_ID, capacity, prerequisites))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("capacity");
    }

    @Test
    void testAllowsOfferingWithoutPrerequisites() {
        Offering open = new Offering(OFFERING_ID, 1, Set.of());

        assertThat(open.enroll(directory.get(FARID))).isEqualTo(new Enrolled(FARID));
    }

    private EnrollResult enroll(LearnerId learnerId) {
        return offering.enroll(directory.get(learnerId));
    }

    private DropResult drop(LearnerId learnerId) {
        return offering.drop(learnerId, this::lookup);
    }

    private Optional<Learner> lookup(LearnerId learnerId) {
        return Optional.ofNullable(directory.get(learnerId));
    }

    private void fillSeats() {
        assertThat(enroll(ASHA)).isEqualTo(new Enrolled(ASHA));
        assertThat(enroll(BEN)).isEqualTo(new Enrolled(BEN));
    }

    private void grantPrerequisite(LearnerId learnerId) {
        directory.put(learnerId, new Learner(learnerId, Set.of(CS101)));
    }

    private void revokePrerequisite(LearnerId learnerId) {
        directory.put(learnerId, new Learner(learnerId, Set.of()));
    }

    private void assertInvariants() {
        OfferingSnapshot snapshot = offering.snapshot();
        assertThat(snapshot.enrolled()).as("I-1 enrolled within capacity")
                .hasSizeLessThanOrEqualTo(snapshot.capacity());
        assertThat(snapshot.enrolled()).as("I-2 roster has no duplicates and no waiting learner")
                .doesNotHaveDuplicates()
                .noneMatch(snapshot.waitlist()::contains);
        assertThat(snapshot.waitlist()).as("I-2 waitlist has no duplicates").doesNotHaveDuplicates();
        if (snapshot.seatsAvailable() > 0) {
            assertThat(snapshot.waitlist()).as("I-3 free seat implies empty waitlist").isEmpty();
        }
    }
}
