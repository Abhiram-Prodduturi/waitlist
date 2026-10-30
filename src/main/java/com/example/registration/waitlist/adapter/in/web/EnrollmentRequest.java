package com.example.registration.waitlist.adapter.in.web;

import com.example.registration.waitlist.domain.Identifiers;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Body of {@code POST /offerings/{offeringId}/enrollments}.
 *
 * @param learnerId learner requesting enrollment
 */
public record EnrollmentRequest(@NotNull @Pattern(regexp = Identifiers.PATTERN_TEXT) String learnerId) {
}
