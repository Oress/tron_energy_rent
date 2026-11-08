package org.ipan.nrgyrent.fixedfloat;

import org.junit.jupiter.api.Test;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FixedFloatRateLimiterTest {
    @Test void expiresEachRequestAfterItsOwnMinuteInsteadOfResettingTheWholeBudget() throws Exception {
        AtomicLong clock = new AtomicLong();
        AtomicInteger waits = new AtomicInteger();
        FixedFloatRateLimiter limiter = new FixedFloatRateLimiter(clock::get, nanos -> {
            waits.incrementAndGet();
            clock.addAndGet(nanos);
        });
        limiter.acquire(50);
        clock.set(TimeUnit.SECONDS.toNanos(30));
        limiter.acquire(200);
        limiter.acquire(50); // At t=60, only the first 50 units expire.
        assertEquals(TimeUnit.SECONDS.toNanos(60), clock.get());
        limiter.acquire(1); // Remaining budget is still full until t=90.
        assertEquals(TimeUnit.SECONDS.toNanos(90), clock.get());
        assertEquals(2, waits.get());
    }
}
