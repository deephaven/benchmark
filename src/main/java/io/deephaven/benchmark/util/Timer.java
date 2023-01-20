package io.deephaven.benchmark.util;

import java.time.Duration;
import java.time.temporal.ChronoUnit;

public class Timer {
	static public long now() {
		return System.currentTimeMillis();
	}
	
	static public Timer start() {
		return new Timer();
	}
	
	final public long beginTime = now();
	
	protected Timer() {
	}
	
	public Duration duration() {
		return Duration.of(now() - beginTime, ChronoUnit.MILLIS);
	}

}
