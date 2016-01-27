package com.bedatadriven.appengine.metrics;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Simple, concurrent but non-blocking rate limiter that ensures
 * that at most one permit is given every {@code n} seconds.
 *
 */
public class NonBlockingRateLimiter {

    private long interval;
    private final AtomicLong lastPermitTick;

    public NonBlockingRateLimiter(long interval, TimeUnit timeUnit) {
        this.lastPermitTick = new AtomicLong(System.nanoTime());
        this.interval = timeUnit.toNanos(interval);
    }

    /**
     * Executes the given task if at least {@code interval} has passed.
     */
    public boolean tryAcquire() {
        final long oldTick = lastPermitTick.get();
        final long newTick = System.nanoTime();
        final long timeSinceLastExecution = newTick - oldTick;
        if (timeSinceLastExecution > interval) {
            if (lastPermitTick.compareAndSet(oldTick, newTick)) {
                return true;
            }
        }
        return false;
    }
}
