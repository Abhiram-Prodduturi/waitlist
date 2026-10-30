package com.example.registration.waitlist.adapter.out.memory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InMemoryOfferingStoreTest {

    private static final OfferingId OFFERING_ID = new OfferingId("CS201-2026F");
    private static final int CAPACITY = 2;
    private static final int THREADS = 16;
    private static final int OPERATIONS = 2_000;
    private static final long TIMEOUT_SECONDS = 5L;

    private final InMemoryOfferingStore store = new InMemoryOfferingStore();

    @BeforeEach
    void setUp() {
        store.add(new Offering(OFFERING_ID, CAPACITY, Set.of()));
    }

    @Test
    void testReturnsTheOperationResult() {
        assertThat(store.withLock(OFFERING_ID, Offering::id)).isEqualTo(OFFERING_ID);
    }

    @Test
    void testThrowsForUnknownOffering() {
        OfferingId unknown = new OfferingId("XX999");
        Function<Offering, OfferingId> operation = Offering::id;

        assertThatThrownBy(() -> store.withLock(unknown, operation))
                .isInstanceOf(OfferingNotFoundException.class)
                .hasMessageContaining("XX999");
    }

    @Test
    void testRejectsDuplicateRegistration() {
        Offering duplicate = new Offering(OFFERING_ID, CAPACITY, Set.of());

        assertThatThrownBy(() -> store.add(duplicate))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void testNeverRunsTwoOperationsOnTheSameOfferingAtOnce() {
        AtomicInteger inside = new AtomicInteger();
        AtomicInteger maxInside = new AtomicInteger();
        AtomicInteger completed = new AtomicInteger();
        CountDownLatch startGate = new CountDownLatch(1);

        try (ExecutorService executor = Executors.newFixedThreadPool(THREADS)) {
            for (int i = 0; i < OPERATIONS; i++) {
                executor.submit(() -> {
                    startGate.await();
                    return store.withLock(OFFERING_ID, offering -> {
                        maxInside.accumulateAndGet(inside.incrementAndGet(), Math::max);
                        Thread.yield();
                        inside.decrementAndGet();
                        return completed.incrementAndGet();
                    });
                });
            }
            startGate.countDown();
        }

        assertThat(completed.get()).as("operations completed").isEqualTo(OPERATIONS);
        assertThat(maxInside.get()).as("operations running at the same time").isEqualTo(1);
    }

    @Test
    void testReleasesTheLockWhenTheOperationThrows() throws Exception {
        Function<Offering, OfferingId> failing = offering -> {
            throw new IllegalStateException("operation failed");
        };
        assertThatThrownBy(() -> store.withLock(OFFERING_ID, failing))
                .isInstanceOf(IllegalStateException.class);

        try (ExecutorService otherThread = Executors.newSingleThreadExecutor()) {
            Future<OfferingId> next = otherThread.submit(() -> store.withLock(OFFERING_ID, Offering::id));

            assertThat(next.get(TIMEOUT_SECONDS, TimeUnit.SECONDS))
                    .as("another thread can acquire the lock")
                    .isEqualTo(OFFERING_ID);
        }
    }
}
