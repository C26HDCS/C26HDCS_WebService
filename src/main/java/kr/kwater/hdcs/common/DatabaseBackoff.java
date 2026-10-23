package kr.kwater.hdcs.common;

import java.util.function.LongSupplier;

/** One probe at a time, with bounded exponential retry after failure. */
public class DatabaseBackoff {
    private final LongSupplier clock;
    private long nextAttempt;
    private long delay;
    private boolean probing;

    public DatabaseBackoff() { this(System::nanoTime); }
    public DatabaseBackoff(LongSupplier clock) { this.clock = clock; }

    public synchronized boolean acquire() {
        if (probing || (delay > 0 && clock.getAsLong() < nextAttempt)) return false;
        probing = true;
        return true;
    }
    public synchronized void success() {
        delay = 0;
        nextAttempt = 0;
        probing = false;
    }
    public synchronized long failure() {
        delay = delay == 0 ? 1000 : Math.min(delay * 2, 60000);
        nextAttempt = clock.getAsLong() + delay * 1_000_000;
        probing = false;
        return delay;
    }
}
