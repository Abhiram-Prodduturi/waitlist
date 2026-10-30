package com.example.registration.waitlist.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.registration.waitlist.adapter.out.memory.InMemoryLearnerDirectory;
import com.example.registration.waitlist.application.EnrollmentService;
import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingSnapshot;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;

class WaitlistConfigurationTest {

    private static final OfferingId OFFERING_ID = new OfferingId("CS201-2026F");
    private static final int CAPACITY = 3;

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(PropertiesConfiguration.class, WaitlistConfiguration.class)
            .withPropertyValues(
                    "app.waitlist.offering.id=CS201-2026F",
                    "app.waitlist.offering.capacity=" + CAPACITY,
                    "app.waitlist.offering.prerequisites[0]=CS101",
                    "app.waitlist.learners[0].id=L-001",
                    "app.waitlist.learners[0].completed-courses[0]=CS101",
                    "app.waitlist.learners[1].id=L-002");

    @Test
    void testSeedsOfferingAndLearnersFromProperties() {
        runner.run(context -> {
            assertThat(context).hasNotFailed().hasSingleBean(EnrollmentService.class);
            OfferingSnapshot snapshot = context.getBean(EnrollmentService.class).view(OFFERING_ID);
            assertThat(snapshot.capacity()).isEqualTo(CAPACITY);
            assertThat(snapshot.prerequisites()).containsExactly(new CourseCode("CS101"));
            InMemoryLearnerDirectory directory = context.getBean(InMemoryLearnerDirectory.class);
            assertThat(directory.find(new LearnerId("L-001"))).get()
                    .satisfies(learner -> assertThat(learner.completedCourses())
                            .containsExactly(new CourseCode("CS101")));
            assertThat(directory.find(new LearnerId("L-002"))).get()
                    .satisfies(learner -> assertThat(learner.completedCourses()).isEmpty());
        });
    }

    @Test
    void testAllowsOfferingWithoutPrerequisites() {
        runner.withPropertyValues("app.waitlist.offering.prerequisites=")
                .run(context -> assertThat(context.getBean(EnrollmentService.class).view(OFFERING_ID)
                        .prerequisites()).isEmpty());
    }

    @ParameterizedTest(name = "{0}={1}")
    @CsvSource({
        "app.waitlist.offering.capacity, 0, offering.capacity",
        "app.waitlist.offering.capacity, -1, offering.capacity",
        "app.waitlist.offering.id, CS 201, offering.id",
        "app.waitlist.learners[1].id, L_002, learners[1].id",
        "app.waitlist.learners[1].id, L-001, Duplicate learner id",
        "app.waitlist.offering.prerequisites[0], CS/101, prerequisites[0]"
    })
    void testFailsStartupOnInvalidSeedData(String property, String value, String expectedReason) {
        runner.withPropertyValues(property + "=" + value)
                .run(context -> assertThat(context).hasFailed()
                        .getFailure()
                        .hasStackTraceContaining(expectedReason));
    }

    @Test
    void testFailsStartupWhenOfferingIsMissing() {
        new ApplicationContextRunner()
                .withUserConfiguration(PropertiesConfiguration.class, WaitlistConfiguration.class)
                .run(context -> assertThat(context).hasFailed());
    }

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(WaitlistProperties.class)
    static class PropertiesConfiguration {
    }
}
