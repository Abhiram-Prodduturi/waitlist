package com.example.registration.waitlist.adapter.out.memory;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import java.util.Set;
import org.junit.jupiter.api.Test;

class InMemoryLearnerDirectoryTest {

    private static final LearnerId ASHA = new LearnerId("L-001");
    private static final CourseCode CS101 = new CourseCode("CS101");

    private final InMemoryLearnerDirectory directory = new InMemoryLearnerDirectory();

    @Test
    void testFindsNothingForUnknownLearner() {
        assertThat(directory.find(ASHA)).isEmpty();
    }

    @Test
    void testReturnsTheLatestRecordAfterReplacement() {
        directory.put(new Learner(ASHA, Set.of(CS101)));
        Learner revoked = new Learner(ASHA, Set.of());

        directory.put(revoked);

        assertThat(directory.find(ASHA)).contains(revoked);
    }
}
