package com.example.registration.waitlist.config;

import com.example.registration.waitlist.domain.Identifiers;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Seed data for the single offering and its learners ({@code app.waitlist.*}). Invalid values fail startup.
 *
 * @param offering the offering to serve
 * @param learners the learner directory contents; IDs must be unique
 */
@Validated
@ConfigurationProperties(prefix = "app.waitlist")
public record WaitlistProperties(@NotNull @Valid OfferingProperties offering, List<@Valid LearnerProperties> learners) {

    public WaitlistProperties {
        learners = learners == null ? List.of() : List.copyOf(learners);
        Set<String> seen = new HashSet<>();
        for (LearnerProperties learner : learners) {
            if (!seen.add(learner.id())) {
                throw new IllegalArgumentException("Duplicate learner id in app.waitlist.learners: " + learner.id());
            }
        }
    }

    /**
     * @param id            offering ID
     * @param capacity      total seats, at least 1
     * @param prerequisites course codes a learner must have completed; may be empty
     */
    public record OfferingProperties(
            @NotNull @Pattern(regexp = Identifiers.PATTERN_TEXT) String id,
            @Min(1) int capacity,
            List<@NotNull @Pattern(regexp = Identifiers.PATTERN_TEXT) String> prerequisites) {

        public OfferingProperties {
            prerequisites = prerequisites == null ? List.of() : List.copyOf(prerequisites);
        }
    }

    /**
     * @param id               learner ID
     * @param completedCourses course codes the learner has completed; may be empty
     */
    public record LearnerProperties(
            @NotNull @Pattern(regexp = Identifiers.PATTERN_TEXT) String id,
            List<@NotNull @Pattern(regexp = Identifiers.PATTERN_TEXT) String> completedCourses) {

        public LearnerProperties {
            completedCourses = completedCourses == null ? List.of() : List.copyOf(completedCourses);
        }
    }
}
