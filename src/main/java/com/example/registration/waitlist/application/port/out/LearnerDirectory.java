package com.example.registration.waitlist.application.port.out;

import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import java.util.Optional;

/**
 * Read access to learner records owned outside this service. Implementations must be thread-safe and
 * return the current record on every call.
 */
public interface LearnerDirectory {

    Optional<Learner> find(LearnerId learnerId);
}
