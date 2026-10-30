package com.example.registration.waitlist.adapter.in.web;

import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.OfferingSnapshot;
import java.util.List;

/**
 * Body of {@code GET /offerings/{offeringId}}.
 *
 * @param offeringId     offering identifier
 * @param capacity       total seats
 * @param seatsAvailable free seats
 * @param prerequisites  required course codes, sorted
 * @param enrolled       enrolled learners in the order they took a seat
 * @param waitlist       waiting learners in FIFO order; index 0 is position 1
 */
public record OfferingResponse(
        String offeringId,
        int capacity,
        int seatsAvailable,
        List<String> prerequisites,
        List<String> enrolled,
        List<String> waitlist) {

    public OfferingResponse {
        prerequisites = List.copyOf(prerequisites);
        enrolled = List.copyOf(enrolled);
        waitlist = List.copyOf(waitlist);
    }

    static OfferingResponse from(OfferingSnapshot snapshot) {
        return new OfferingResponse(
                snapshot.id().value(),
                snapshot.capacity(),
                snapshot.seatsAvailable(),
                snapshot.prerequisites().stream().sorted().map(CourseCode::value).toList(),
                snapshot.enrolled().stream().map(LearnerId::value).toList(),
                snapshot.waitlist().stream().map(LearnerId::value).toList());
    }
}
