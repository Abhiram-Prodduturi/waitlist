package com.example.registration.waitlist.adapter.out.memory;

import com.example.registration.waitlist.application.port.out.OfferingStore;
import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Function;

/**
 * In-memory offerings, each guarded by its own fair {@link ReentrantLock}. Thread-safe.
 *
 * <p>The lock is fair so waiting requests acquire it roughly in arrival order; waitlist order is the order in
 * which requests acquire the lock (DESIGN.md section 9).
 */
public final class InMemoryOfferingStore implements OfferingStore {

    private final Map<OfferingId, GuardedOffering> offerings = new ConcurrentHashMap<>();

    /**
     * Registers an offering.
     *
     * @throws IllegalStateException if an offering with the same ID is already registered
     */
    public void add(Offering offering) {
        Objects.requireNonNull(offering, "offering");
        GuardedOffering guarded = new GuardedOffering(offering, new ReentrantLock(true));
        if (offerings.putIfAbsent(offering.id(), guarded) != null) {
            throw new IllegalStateException("Offering " + offering.id() + " is already registered");
        }
    }

    @Override
    public <R> R withLock(OfferingId offeringId, Function<Offering, R> operation) {
        Objects.requireNonNull(operation, "operation");
        GuardedOffering guarded = offerings.get(Objects.requireNonNull(offeringId, "offeringId"));
        if (guarded == null) {
            throw new OfferingNotFoundException(offeringId);
        }
        guarded.lock().lock();
        try {
            return operation.apply(guarded.offering());
        } finally {
            guarded.lock().unlock();
        }
    }

    private record GuardedOffering(Offering offering, ReentrantLock lock) {
    }
}
