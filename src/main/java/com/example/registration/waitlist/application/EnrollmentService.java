package com.example.registration.waitlist.application;

import com.example.registration.waitlist.application.port.out.LearnerDirectory;
import com.example.registration.waitlist.application.port.out.OfferingStore;
import com.example.registration.waitlist.domain.DropResult;
import com.example.registration.waitlist.domain.EnrollResult;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.LearnerNotFoundException;
import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import com.example.registration.waitlist.domain.OfferingSnapshot;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enroll, drop, and read use cases for one offering. Thread-safe: every request runs entirely inside the
 * offering's lock, including the learner lookups, so eligibility is read at the moment it is applied.
 */
public final class EnrollmentService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EnrollmentService.class);

    private final OfferingStore offeringStore;
    private final LearnerDirectory learnerDirectory;

    public EnrollmentService(OfferingStore offeringStore, LearnerDirectory learnerDirectory) {
        this.offeringStore = Objects.requireNonNull(offeringStore, "offeringStore");
        this.learnerDirectory = Objects.requireNonNull(learnerDirectory, "learnerDirectory");
    }

    /**
     * Applies an enroll request.
     *
     * @throws OfferingNotFoundException if the offering does not exist
     * @throws LearnerNotFoundException  if the learner is not in the directory
     */
    public EnrollResult enroll(OfferingId offeringId, LearnerId learnerId) {
        Objects.requireNonNull(learnerId, "learnerId");
        return offeringStore.withLock(offeringId, offering -> {
            Learner learner = requireLearner(learnerId);
            EnrollResult result = offering.enroll(learner);
            logEnroll(offeringId, result);
            return result;
        });
    }

    /**
     * Applies a drop request, running promotion if a seat opens.
     *
     * @throws OfferingNotFoundException if the offering does not exist
     * @throws LearnerNotFoundException  if the learner is not in the directory
     */
    public DropResult drop(OfferingId offeringId, LearnerId learnerId) {
        Objects.requireNonNull(learnerId, "learnerId");
        return offeringStore.withLock(offeringId, offering -> {
            requireLearner(learnerId);
            DropResult result = offering.drop(learnerId, learnerDirectory::find);
            logDrop(offeringId, result);
            return result;
        });
    }

    /**
     * Returns a consistent copy of the offering's state.
     *
     * @throws OfferingNotFoundException if the offering does not exist
     */
    public OfferingSnapshot view(OfferingId offeringId) {
        return offeringStore.withLock(offeringId, Offering::snapshot);
    }

    private Learner requireLearner(LearnerId learnerId) {
        return learnerDirectory.find(learnerId).orElseThrow(() -> new LearnerNotFoundException(learnerId));
    }

    private static void logEnroll(OfferingId offeringId, EnrollResult result) {
        switch (result) {
            case EnrollResult.Enrolled enrolled ->
                    LOGGER.info("Enrolled learner={} offering={}", enrolled.learnerId(), offeringId);
            case EnrollResult.Waitlisted waitlisted ->
                    LOGGER.info("Waitlisted learner={} offering={} position={}",
                            waitlisted.learnerId(), offeringId, waitlisted.position());
            default ->
                    LOGGER.debug("No change for learner={} offering={} outcome={}",
                            result.learnerId(), offeringId, result.getClass().getSimpleName());
        }
    }

    private static void logDrop(OfferingId offeringId, DropResult result) {
        switch (result) {
            case DropResult.Dropped dropped -> {
                LOGGER.info("Dropped learner={} offering={}", dropped.learnerId(), offeringId);
                dropped.removedIneligibleLearners().forEach(removed ->
                        LOGGER.info("Removed ineligible learner={} from waitlist offering={}", removed, offeringId));
                dropped.promotedLearner().ifPresent(promoted ->
                        LOGGER.info("Promoted learner={} offering={}", promoted, offeringId));
            }
            case DropResult.LeftWaitlist left ->
                    LOGGER.info("Learner={} left waitlist offering={}", left.learnerId(), offeringId);
            default ->
                    LOGGER.debug("No change for learner={} offering={} outcome={}",
                            result.learnerId(), offeringId, result.getClass().getSimpleName());
        }
    }
}
