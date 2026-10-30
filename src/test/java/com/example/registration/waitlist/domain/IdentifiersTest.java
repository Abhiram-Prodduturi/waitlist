package com.example.registration.waitlist.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

class IdentifiersTest {

    private static final int MAX_LENGTH = 64;

    static Stream<String> validValues() {
        return Stream.of("L-001", "a", "CS201-2026F", "A".repeat(MAX_LENGTH));
    }

    static Stream<String> invalidValues() {
        return Stream.of("", " ", "L 001", "L_001", "L-001\n", "L/001", "A".repeat(MAX_LENGTH + 1));
    }

    @ParameterizedTest
    @MethodSource("validValues")
    void testAcceptsWellFormedIdentifiers(String value) {
        assertThat(new LearnerId(value)).hasToString(value);
        assertThat(new OfferingId(value)).hasToString(value);
        assertThat(new CourseCode(value)).hasToString(value);
    }

    @ParameterizedTest
    @MethodSource("invalidValues")
    void testRejectsMalformedIdentifiers(String value) {
        assertThatThrownBy(() -> new LearnerId(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("learnerId");
    }

    @Test
    void testRejectsNullIdentifier() {
        assertThatThrownBy(() -> new OfferingId(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("offeringId");
    }
}
