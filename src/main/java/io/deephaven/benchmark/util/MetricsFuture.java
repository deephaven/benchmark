package io.deephaven.benchmark.util;

import java.util.concurrent.*;

public class MetricsFuture implements Future<Metrics> {
	final private CountDownLatch latch = new CountDownLatch(1);
	final private Metrics metrics;
	
	public MetricsFuture(Metrics metrics) {
		this.metrics = metrics;
	}
	
	public void done() {
		latch.countDown();
	}
	
	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}

	@Override
	public boolean isCancelled() {
		return latch.getCount() < 1;
	}

	@Override
	public boolean isDone() {
		return latch.getCount() < 1;
	}

	@Override
	public Metrics get() throws InterruptedException, ExecutionException {
		latch.await();
		return metrics;
	}

	@Override
	public Metrics get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		latch.await(timeout, unit);
		return metrics;
	}
	
}
