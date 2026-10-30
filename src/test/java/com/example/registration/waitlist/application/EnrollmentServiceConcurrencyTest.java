package com.example.registration.waitlist.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.registration.waitlist.adapter.out.memory.InMemoryLearnerDirectory;
import com.example.registration.waitlist.adapter.out.memory.InMemoryOfferingStore;
import com.example.registration.waitlist.domain.CourseCode;
import com.example.registration.waitlist.domain.DropResult;
import com.example.registration.waitlist.domain.EnrollResult;
import com.example.registration.waitlist.domain.Learner;
import com.example.registration.waitlist.domain.LearnerId;
import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingSnapshot;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Concurrent requests against one offering (NFR-1, NFR-2). All tasks wait on a start gate so they contend
 * for the offering lock at the same time.
 */
class EnrollmentServiceConcurrencyTest {

    private static final OfferingId OFFERING_ID = new OfferingId("CS201-2026F");
    private static final CourseCode CS101 = new CourseCode("CS101");
    private static final int CAPACITY = 5;
    private static final int THREADS = 32;
    private static final int LEARNERS = 200;
    private static final int DUPLICATE_REQUESTS = 50;
    private static final int INITIAL_WAITLIST = 20;
    private static final int LATE_ARRIVALS = 10;

    private final InMemoryLearnerDirectory directory = new InMemoryLearnerDirectory();
    private EnrollmentService service;

    @BeforeEach
    void setUp() {
        InMemoryOfferingStore store = new InMemoryOfferingStore();
        store.add(new Offering(OFFERING_ID, CAPACITY, Set.of(CS101)));
        service = new EnrollmentService(store, directory);
    }

    @Test
    @DisplayName("Concurrent enrolls fill exactly the capacity and assign every waitlist position once")
    void testConcurrentEnrollsNeverExceedCapacity() throws Exception {
        List<LearnerId> learners = registerLearners("A", LEARNERS);
        List<Callable<EnrollResult>> tasks = learners.stream()
                .<Callable<EnrollResult>>map(learnerId -> () -> service.enroll(OFFERING_ID, learnerId))
                .toList();

        List<EnrollResult> results = runConcurrently(tasks);

        OfferingSnapshot snapshot = service.view(OFFERING_ID);
        assertThat(results).filteredOn(EnrollResult.Enrolled.class::isInstance).hasSize(CAPACITY);
        assertThat(results).filteredOn(EnrollResult.Waitlisted.class::isInstance).hasSize(LEARNERS - CAPACITY);
        assertThat(snapshot.enrolled()).hasSize(CAPACITY).doesNotHaveDuplicates();
        assertThat(snapshot.waitlist()).hasSize(LEARNERS - CAPACITY).doesNotHaveDuplicates();
        for (EnrollResult result : results) {
            if (result instanceof EnrollResult.Waitlisted waitlisted) {
                assertThat(snapshot.waitlist().get(waitlisted.position() - 1))
                        .as("learner reported at position %d", waitlisted.position())
                        .isEqualTo(waitlisted.learnerId());
            }
        }
    }

    @Test
    @DisplayName("Concurrent duplicate requests from one learner create one record")
    void testConcurrentDuplicateEnrollsCreateOneRecord() throws Exception {
        LearnerId learnerId = registerLearners("D", 1).getFirst();
        List<Callable<EnrollResult>> tasks = IntStream.range(0, DUPLICATE_REQUESTS)
                .<Callable<EnrollResult>>mapToObj(i -> () -> service.enroll(OFFERING_ID, learnerId))
                .toList();

        List<EnrollResult> results = runConcurrently(tasks);

        assertThat(results).filteredOn(EnrollResult.Enrolled.class::isInstance).hasSize(1);
        assertThat(results).filteredOn(EnrollResult.AlreadyEnrolled.class::isInstance)
                .hasSize(DUPLICATE_REQUESTS - 1);
        assertThat(service.view(OFFERING_ID).enrolled()).containsExactly(learnerId);
    }

    @Test
    @DisplayName("Concurrent drops promote in FIFO order, repeated drops are no-ops, newcomers queue behind")
    void testConcurrentDropsPromoteInFifoOrder() throws Exception {
        List<LearnerId> seated = registerLearners("S", CAPACITY);
        List<LearnerId> waiting = registerLearners("W", INITIAL_WAITLIST);
        List<LearnerId> newcomers = registerLearners("N", LATE_ARRIVALS);
        seated.forEach(learnerId -> service.enroll(OFFERING_ID, learnerId));
        waiting.forEach(learnerId -> service.enroll(OFFERING_ID, learnerId));

        List<Callable<Object>> tasks = new ArrayList<>();
        for (LearnerId learnerId : seated) {
            tasks.add(() -> service.drop(OFFERING_ID, learnerId));
            tasks.add(() -> service.drop(OFFERING_ID, learnerId));
        }
        for (LearnerId learnerId : newcomers) {
            tasks.add(() -> service.enroll(OFFERING_ID, learnerId));
        }

        List<Object> results = runConcurrently(tasks);

        assertThat(results).filteredOn(DropResult.Dropped.class::isInstance).hasSize(CAPACITY);
        assertThat(results).filteredOn(DropResult.NotEnrolled.class::isInstance).hasSize(CAPACITY);
        assertThat(results).filteredOn(EnrollResult.Waitlisted.class::isInstance).hasSize(LATE_ARRIVALS);
        OfferingSnapshot snapshot = service.view(OFFERING_ID);
        assertThat(snapshot.enrolled()).as("first five waiting learners, in queue order")
                .containsExactlyElementsOf(waiting.subList(0, CAPACITY));
        assertThat(snapshot.waitlist().subList(0, INITIAL_WAITLIST - CAPACITY))
                .as("remaining original waiters keep their order")
                .containsExactlyElementsOf(waiting.subList(CAPACITY, INITIAL_WAITLIST));
        assertThat(snapshot.waitlist().subList(INITIAL_WAITLIST - CAPACITY, snapshot.waitlist().size()))
                .as("newcomers join behind everyone already waiting")
                .containsExactlyInAnyOrderElementsOf(newcomers);
    }

    private List<LearnerId> registerLearners(String prefix, int count) {
        List<LearnerId> learners = IntStream.rangeClosed(1, count)
                .mapToObj(i -> new LearnerId("L-" + prefix + i))
                .toList();
        learners.forEach(learnerId -> directory.put(new Learner(learnerId, Set.of(CS101))));
        return learners;
    }

    private static <T> List<T> runConcurrently(List<Callable<T>> tasks) throws Exception {
        CountDownLatch startGate = new CountDownLatch(1);
        List<Future<T>> futures = new ArrayList<>();
        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (Callable<T> task : tasks) {
                futures.add(executor.submit(() -> {
                    startGate.await();
                    return task.call();
                }));
            }
            startGate.countDown();
        }
        List<T> results = new ArrayList<>();
        for (Future<T> future : futures) {
            results.add(future.get());
        }
        return results;
    }
}
