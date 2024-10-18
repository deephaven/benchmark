/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.metric;

import java.util.concurrent.*;

/**
 * A <code>Future</code> that contains the metrics collected during the task that the future corresponds to.
 */
public class MetricsFuture implements Future<Metrics> {
    final private CountDownLatch latch = new CountDownLatch(1);
    final private Metrics metrics;

    /**
     * Initialize the future with a container to house metrics added during the corresponding task
     * 
     * @param metrics the container where the metrics will be added
     */
    public MetricsFuture(Metrics metrics) {
        this.metrics = metrics;
    }

    /**
     * Tell the future it's done
     */
    public void done() {
        latch.countDown();
    }

    /**
     * Cancel the task for this future (Always fails)
     * 
     * @return false
     */
    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    /**
     * Since this future can't be cancelled, this works the same as <code>isDone</code>.
     * 
     * @return true if done, otherwise false
     */
    @Override
    public boolean isCancelled() {
        return latch.getCount() < 1;
    }

    /**
     * Get whether or not the task is finished executing.
     * 
     * @return true if the task is finished, otherwise false
     */
    @Override
    public boolean isDone() {
        return latch.getCount() < 1;
    }

    /**
     * Get the metrics set for the task. Wait until the task is done.
     * 
     * @return the metrics provided with the task
     */
    @Override
    public Metrics get() throws InterruptedException, ExecutionException {
        latch.await();
        return metrics;
    }

    /**
     * Get the metrics set for the task. Wait as long as the given timeout before.
     * 
     * @return the metrics provided with the task
     */
    @Override
    public Metrics get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        latch.await(timeout, unit);
        return metrics;
    }

}
