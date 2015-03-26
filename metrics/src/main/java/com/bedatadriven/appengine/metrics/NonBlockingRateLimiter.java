package com.bedatadriven.appengine.metrics;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Ticker;

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
    private Ticker ticker;

    @VisibleForTesting
    public NonBlockingRateLimiter(Ticker ticker, long interval, TimeUnit timeUnit) {
        this.ticker = ticker;
        this.lastPermitTick = new AtomicLong(ticker.read());
        this.interval = timeUnit.toNanos(interval);
    }

    public NonBlockingRateLimiter(long interval, TimeUnit timeUnit) {
        this(Ticker.systemTicker(), interval, timeUnit);
    }

    /**
     * Executes the given task if at least {@code interval} has passed.
     */
    public boolean tryAcquire() {
        final long oldTick = lastPermitTick.get();
        final long newTick = ticker.read();
        final long timeSinceLastExecution = newTick - oldTick;
        if (timeSinceLastExecution > interval) {
            if (lastPermitTick.compareAndSet(oldTick, newTick)) {
                return true;
            }
        }
        return false;
    }
}
