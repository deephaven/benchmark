package io.deephaven.benchmark.util;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class Threads {

	static public ExecutorService single(String threadName) {
		return Executors.newSingleThreadExecutor(new ThreadFactory() {
			@Override
			public Thread newThread(Runnable r) {
				return new Thread(r, threadName);
			}	
		});
	}
	
	static public void sleep(long millis) {
		try { Thread.sleep(millis); } 
		catch (InterruptedException e) {}
	}
	
}
