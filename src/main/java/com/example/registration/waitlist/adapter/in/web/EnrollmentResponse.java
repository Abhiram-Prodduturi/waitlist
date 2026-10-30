package com.example.registration.waitlist.adapter.in.web;

import com.example.registration.waitlist.domain.DropResult;
import com.example.registration.waitlist.domain.EnrollResult;
import com.example.registration.waitlist.domain.LearnerId;
import java.util.List;

/**
 * Body of every successful (2xx) enroll or drop response. Fields that do not apply to the outcome are null
 * (or an empty list), so every response has the same shape.
 *
 * @param outcome                     what happened
 * @param learnerId                   the learner in the request
 * @param waitlistPosition            1-based position for {@code WAITLISTED} and {@code ALREADY_WAITLISTED}
 * @param promotedLearnerId           learner moved into the freed seat, for {@code DROPPED}
 * @param removedIneligibleLearnerIds learners removed from the waitlist as ineligible, for {@code DROPPED}
 */
public record EnrollmentResponse(
        Outcome outcome,
        String learnerId,
        Integer waitlistPosition,
        String promotedLearnerId,
        List<String> removedIneligibleLearnerIds) {

    public EnrollmentResponse {
        removedIneligibleLearnerIds = List.copyOf(removedIneligibleLearnerIds);
    }

    static EnrollmentResponse of(Outcome outcome, LearnerId learnerId) {
        return new EnrollmentResponse(outcome, learnerId.value(), null, null, List.of());
    }

    static EnrollmentResponse waiting(Outcome outcome, LearnerId learnerId, int position) {
        return new EnrollmentResponse(outcome, learnerId.value(), position, null, List.of());
    }

    static EnrollmentResponse from(EnrollResult.Enrolled result) {
        return of(Outcome.ENROLLED, result.learnerId());
    }

    static EnrollmentResponse from(DropResult.Dropped result) {
        return new EnrollmentResponse(
                Outcome.DROPPED,
                result.learnerId().value(),
                null,
                result.promotedLearner().map(LearnerId::value).orElse(null),
                result.removedIneligibleLearners().stream().map(LearnerId::value).toList());
    }
}
