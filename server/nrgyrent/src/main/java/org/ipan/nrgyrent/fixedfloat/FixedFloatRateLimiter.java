package org.ipan.nrgyrent.fixedfloat;

import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/** Shared by creation and polling in this client instance. See https://ff.io/api. */
final class FixedFloatRateLimiter {
    private static final int CAPACITY = 250;
    private static final long WINDOW_NANOS = TimeUnit.MINUTES.toNanos(1);
    private final Deque<Usage> requests = new ArrayDeque<>();
    private final LongSupplier clock;
    private final Sleeper sleeper;
    private int used;

    FixedFloatRateLimiter() {
        this(System::nanoTime, TimeUnit.NANOSECONDS::sleep);
    }

    FixedFloatRateLimiter(LongSupplier clock, Sleeper sleeper) {
        this.clock = clock;
        this.sleeper = sleeper;
    }

    void acquire(int weight) throws InterruptedIOException {
        if (weight < 1 || weight > CAPACITY) throw new IllegalArgumentException("Invalid FixedFloat request weight");
        while (true) {
            if (Thread.currentThread().isInterrupted()) throw new InterruptedIOException("Interrupted waiting for FixedFloat request budget");
            long waitNanos;
            synchronized (this) {
                long now = clock.getAsLong();
                while (!requests.isEmpty() && now - requests.peekFirst().time() >= WINDOW_NANOS) {
                    used -= requests.removeFirst().weight();
                }
                if (used + weight <= CAPACITY) {
                    requests.addLast(new Usage(now, weight));
                    used += weight;
                    return;
                }
                waitNanos = WINDOW_NANOS - (now - requests.peekFirst().time());
            }
            try {
                // Do not hold the lock while waiting: a lighter status request may still fit.
                sleeper.sleep(waitNanos);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException("Interrupted waiting for FixedFloat request budget");
            }
        }
    }

    @FunctionalInterface
    interface Sleeper {
        void sleep(long nanos) throws InterruptedException;
    }

    private record Usage(long time, int weight) {}
}
