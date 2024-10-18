/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * Provide help for using threads
 */
public class Threads {

    /**
     * Make a named <code>ExecutorService</code> that uses a single thread
     * 
     * @param threadName the thread name
     * @return an <code>ExecutorService</code> service with one thread
     */
    static public ExecutorService single(String threadName) {
        return Executors.newSingleThreadExecutor(new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, threadName);
            }
        });
    }

    /**
     * Pause the current thread of the given amount of milliseconds
     * 
     * @param millis how long in millis to pause
     */
    static public void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
        }
    }

}
