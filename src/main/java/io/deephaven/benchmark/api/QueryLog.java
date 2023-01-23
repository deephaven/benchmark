package io.deephaven.benchmark.api;

import static java.nio.file.StandardOpenOption.*;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

class QueryLog implements Closeable {
	final Class<?> testClass;
	final Path parent;
	final Path logFile;
	final List<String> queries = new ArrayList<>();
	private String name = null;
	private boolean isClosed = false;
	
	QueryLog(Path parent, Class<?> testClass) {
		this.testClass = testClass;
		this.parent = parent;
		this.logFile = getLogFile(parent, testClass);
	}
	
	public void close() {
		if(isClosed) return;
		isClosed = true;
		
		if(queries.isEmpty()) return;
		if(!Files.exists(logFile)) {
			write("# Test Class - " + testClass.getName(), 2);
		}
		write("## Test - " + name, 2);
		for(int i = 0, n = queries.size(); i < n; i++) {
			write("### Query " + (i+1), 1);
			write("````", 1);
			write(queries.get(i), 0);
			write("````", 2);
		}
	}
	
	void setName(String name) {
		this.name = name;
	}
	
	void logQuery(String query) {
		if(name == null) throw new RuntimeException("Set a test name before logging a query");
		if(isClosed) throw new RuntimeException("Attempted to log query to close Query Log");
		queries.add(query);
	}
	
	private void write(String text, int newLineCount) {
		try(BufferedWriter out = Files.newBufferedWriter(logFile, CREATE, APPEND)) {
			out.write(text);
			for(int i = 0; i < newLineCount; i++) out.newLine();
		} catch(Exception ex) {
			throw new RuntimeException("Failed to write to query log: " + logFile, ex);
		}
	}
	
	static Path getLogFile(Path parent, Class<?> testClass) {
		Path logFile = parent.resolve("test-logs/" + testClass.getName() + ".query.md");
		try {
			Files.createDirectories(logFile.getParent());
			return logFile;
		} catch(Exception ex) {
			throw new RuntimeException("Failed to create query log directory" + logFile.getParent(), ex);
		}
	}

}
