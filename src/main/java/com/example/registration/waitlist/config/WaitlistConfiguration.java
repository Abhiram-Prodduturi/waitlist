package com.example.registration.waitlist.config;

import com.example.registration.waitlist.adapter.out.memory.InMemoryLearnerDirectory;
import com.example.registration.waitlist.adapter.out.memory.InMemoryOfferingStore;
import com.example.registration.waitlist.application.EnrollmentService;
import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Builds the in-memory stores from {@link WaitlistProperties} and wires the enrollment service.
 */
@Configuration(proxyBeanMethods = false)
public class WaitlistConfiguration {

    @Bean
    InMemoryLearnerDirectory learnerDirectory(WaitlistProperties properties) {
        InMemoryLearnerDirectory directory = new InMemoryLearnerDirectory();
        for (WaitlistProperties.LearnerProperties learner : properties.learners()) {
            directory.put(new Learner(new LearnerId(learner.id()), toCourseCodes(learner.completedCourses())));
        }
        return directory;
    }

    @Bean
    InMemoryOfferingStore offeringStore(WaitlistProperties properties) {
        WaitlistProperties.OfferingProperties offering = properties.offering();
        InMemoryOfferingStore store = new InMemoryOfferingStore();
        store.add(new Offering(
                new OfferingId(offering.id()), offering.capacity(), toCourseCodes(offering.prerequisites())));
        return store;
    }

    @Bean
    EnrollmentService enrollmentService(InMemoryOfferingStore offeringStore,
            InMemoryLearnerDirectory learnerDirectory) {
        return new EnrollmentService(offeringStore, learnerDirectory);
    }

    private static Set<CourseCode> toCourseCodes(Collection<String> codes) {
        return codes.stream().map(CourseCode::new).collect(Collectors.toUnmodifiableSet());
    }
}
