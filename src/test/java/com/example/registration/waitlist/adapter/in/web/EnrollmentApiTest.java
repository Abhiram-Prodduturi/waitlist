package com.example.registration.waitlist.adapter.in.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.registration.waitlist.adapter.out.memory.InMemoryLearnerDirectory;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * End-to-end HTTP contract for every outcome row in DESIGN.md section 3, against the real wiring and the seeded
 * {@code application.yml}: offering CS201-2026F, 2 seats, prerequisite CS101; L-001..L-004 have CS101, L-005
 * has nothing. The context is rebuilt after each test because the in-memory state is shared.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class EnrollmentApiTest {

    private static final String OFFERING = "/offerings/CS201-2026F";
    private static final String ENROLLMENTS = OFFERING + "/enrollments";
    private static final String OUTCOME = "$.outcome";
    private static final String POSITION = "$.waitlistPosition";
    private static final String ASHA = "L-001";
    private static final String BEN = "L-002";
    private static final String CHEN = "L-003";
    private static final String DEV = "L-004";
    private static final String NO_PREREQUISITE = "L-005";

    private final MockMvc mockMvc;
    private final InMemoryLearnerDirectory learnerDirectory;

    @Autowired
    EnrollmentApiTest(MockMvc mockMvc, InMemoryLearnerDirectory learnerDirectory) {
        this.mockMvc = mockMvc;
        this.learnerDirectory = learnerDirectory;
    }

    @Test
    void testEnrollWithFreeSeatReturns201() throws Exception {
        enroll(ASHA)
                .andExpect(status().isCreated())
                .andExpect(jsonPath(OUTCOME).value("ENROLLED"))
                .andExpect(jsonPath("$.learnerId").value(ASHA))
                .andExpect(jsonPath(POSITION).doesNotExist());
    }

    @Test
    void testEnrollWhenFullReturns202WithPosition() throws Exception {
        fillSeats();

        enroll(CHEN)
                .andExpect(status().isAccepted())
                .andExpect(jsonPath(OUTCOME).value("WAITLISTED"))
                .andExpect(jsonPath(POSITION).value(1));
    }

    @Test
    void testDuplicateEnrollRequestsReturn200WithoutNewRecords() throws Exception {
        fillSeats();
        enroll(CHEN);

        enroll(ASHA).andExpect(status().isOk()).andExpect(jsonPath(OUTCOME).value("ALREADY_ENROLLED"));
        enroll(CHEN).andExpect(status().isOk())
                .andExpect(jsonPath(OUTCOME).value("ALREADY_WAITLISTED"))
                .andExpect(jsonPath(POSITION).value(1));
        view().andExpect(jsonPath("$.enrolled.length()").value(2))
                .andExpect(jsonPath("$.waitlist.length()").value(1));
    }

    @Test
    void testPrerequisiteFailureReturns422ProblemDetail() throws Exception {
        enroll(NO_PREREQUISITE)
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(OUTCOME).value("PREREQUISITES_NOT_MET"))
                .andExpect(jsonPath("$.missingPrerequisites[0]").value("CS101"))
                .andExpect(jsonPath("$.status").value(422));
        view().andExpect(jsonPath("$.seatsAvailable").value(2))
                .andExpect(jsonPath("$.waitlist.length()").value(0));
    }

    @Test
    void testDropPromotesEarliestWaitlistedLearner() throws Exception {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);

        drop(ASHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath(OUTCOME).value("DROPPED"))
                .andExpect(jsonPath("$.promotedLearnerId").value(CHEN))
                .andExpect(jsonPath("$.removedIneligibleLearnerIds.length()").value(0));
        view().andExpect(jsonPath("$.enrolled[0]").value(BEN))
                .andExpect(jsonPath("$.enrolled[1]").value(CHEN))
                .andExpect(jsonPath("$.waitlist[0]").value(DEV));
    }

    @Test
    void testDropRemovesIneligibleHeadAndPromotesNext() throws Exception {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        learnerDirectory.put(new Learner(new LearnerId(CHEN), Set.of()));

        drop(ASHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.promotedLearnerId").value(DEV))
                .andExpect(jsonPath("$.removedIneligibleLearnerIds[0]").value(CHEN));
        view().andExpect(jsonPath("$.waitlist.length()").value(0));
    }

    @Test
    void testDropWhileWaitlistedLeavesWaitlist() throws Exception {
        fillSeats();
        enroll(CHEN);

        drop(CHEN).andExpect(status().isOk()).andExpect(jsonPath(OUTCOME).value("LEFT_WAITLIST"));
        view().andExpect(jsonPath("$.enrolled.length()").value(2))
                .andExpect(jsonPath("$.waitlist.length()").value(0));
    }

    @Test
    void testRepeatedDropReturnsNotEnrolledAndPromotesNobody() throws Exception {
        fillSeats();
        enroll(CHEN);
        enroll(DEV);
        drop(ASHA);

        drop(ASHA)
                .andExpect(status().isOk())
                .andExpect(jsonPath(OUTCOME).value("NOT_ENROLLED"))
                .andExpect(jsonPath("$.promotedLearnerId").doesNotExist());
        view().andExpect(jsonPath("$.waitlist[0]").value(DEV))
                .andExpect(jsonPath("$.seatsAvailable").value(0));
    }

    @Test
    void testUnknownLearnerReturns404() throws Exception {
        enroll("L-999")
                .andExpect(status().isNotFound())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(OUTCOME).value("LEARNER_NOT_FOUND"));
        drop("L-999").andExpect(status().isNotFound()).andExpect(jsonPath(OUTCOME).value("LEARNER_NOT_FOUND"));
    }

    @Test
    void testUnknownOfferingReturns404() throws Exception {
        mockMvc.perform(get("/offerings/XX999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath(OUTCOME).value("OFFERING_NOT_FOUND"));
    }

    @ParameterizedTest(name = "body {0}")
    @ValueSource(strings = {"{\"learnerId\":\"L 001\"}", "{\"learnerId\":\"\"}", "{}", "not json", ""})
    void testInvalidEnrollBodyReturns400(String body) throws Exception {
        mockMvc.perform(post(ENROLLMENTS).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath(OUTCOME).value("INVALID_REQUEST"));
        view().andExpect(jsonPath("$.seatsAvailable").value(2));
    }

    @Test
    void testInvalidPathIdentifierReturns400() throws Exception {
        drop("L_001").andExpect(status().isBadRequest()).andExpect(jsonPath(OUTCOME).value("INVALID_REQUEST"));
        mockMvc.perform(get("/offerings/CS_201"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath(OUTCOME).value("INVALID_REQUEST"));
    }

    @Test
    void testViewReturnsOfferingState() throws Exception {
        fillSeats();
        enroll(CHEN);

        view()
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.offeringId").value("CS201-2026F"))
                .andExpect(jsonPath("$.capacity").value(2))
                .andExpect(jsonPath("$.seatsAvailable").value(0))
                .andExpect(jsonPath("$.prerequisites[0]").value("CS101"))
                .andExpect(jsonPath("$.waitlist[0]").value(CHEN));
    }

    private void fillSeats() throws Exception {
        enroll(ASHA).andExpect(status().isCreated());
        enroll(BEN).andExpect(status().isCreated());
    }

    private ResultActions enroll(String learnerId) throws Exception {
        return mockMvc.perform(post(ENROLLMENTS)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"learnerId\":\"" + learnerId + "\"}"));
    }

    private ResultActions drop(String learnerId) throws Exception {
        return mockMvc.perform(delete(ENROLLMENTS + "/" + learnerId));
    }

    private ResultActions view() throws Exception {
        return mockMvc.perform(get(OFFERING));
    }
}
