/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

/**
 * Timer used to get the duration of a block of code. Precision is in milliseconds.
 */
public class Timer {
    /**
     * Get the elapsed time since epoch in milliseconds
     * 
     * @return current time epoch
     */
    static public long now() {
        return System.currentTimeMillis();
    }

    /**
     * Start the timer
     * 
     * @return a <code>Timer</code> instance initialized to the current time
     */
    static public Timer start() {
        return new Timer();
    }

    /**
     * The time the <code>Timer</code> was initialized
     */
    final public long beginTime = now();

    protected Timer() {}

    /**
     * The elapsed time in milliseconds between now and the beginning when this <code>Timer</code> was initialized
     * 
     * @return time elapsed since start
     */
    public Duration duration() {
        return Duration.of(now() - beginTime, ChronoUnit.MILLIS);
    }

}
