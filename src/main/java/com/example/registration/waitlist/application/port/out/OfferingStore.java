package com.example.registration.waitlist.application.port.out;

import com.example.registration.waitlist.domain.Offering;
import com.example.registration.waitlist.domain.OfferingId;
import com.example.registration.waitlist.domain.OfferingNotFoundException;
import java.util.function.Function;

/**
 * Holds offerings and gives exclusive access to one offering at a time.
 *
 * <p>Contract: {@link #withLock} runs the operation while no other operation on the same offering is running,
 * so a whole check-then-act sequence is atomic. Implementations must be thread-safe. A database-backed
 * implementation would run the operation in a transaction holding a row lock.
 */
public interface OfferingStore {

    /**
     * Runs {@code operation} against the offering while holding its exclusive lock, and returns its result.
     *
     * @throws OfferingNotFoundException if no offering has this ID
     */
    <R> R withLock(OfferingId offeringId, Function<Offering, R> operation);
}
