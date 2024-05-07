/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import static java.nio.file.StandardOpenOption.*;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains the queries used through the API to run tests. Collected queries are written to a Markdown file in the
 * benchmark results directory
 */
class QueryLog implements Closeable {
    final Class<?> testClass;
    final Path parent;
    final Path logFile;
    final List<String> queries = new ArrayList<>();
    private String name = null;
    private boolean isClosed = false;

    /**
     * Initialize the query log according to the test class it's tracking
     * 
     * @param parent the parent directory of the query log
     * @param testClass the test class instance being tracked
     */
    QueryLog(Path parent, Class<?> testClass) {
        this.testClass = testClass;
        this.parent = parent;
        this.logFile = getLogFile(parent, testClass);
    }

    /**
     * Flush all data to the query log and close it
     */
    public void close() {
        if (isClosed)
            return;
        isClosed = true;

        if (queries.isEmpty())
            return;
        if (!Files.exists(logFile)) {
            write("# Test Class - " + testClass.getName(), 2);
        }
        var label = name.startsWith("#") ? "Setup" : "Test";
        name = name.replaceAll("^[#]", "").trim();
        write("## " + label + " - " + name, 2);
        for (int i = 0, n = queries.size(); i < n; i++) {
            write("### Query " + (i + 1), 1);
            write("````", 1);
            write(queries.get(i), 0);
            write("````", 2);
        }
    }

    /**
     * Set the name of the current test. The query log records queries for a test class and denotes queries according to
     * user-supplied test names.
     * <p/>
     * Note: The special character "#" is used to denote that this name is not a test name. This log uses it to denote
     * test setup, while other file handlers, like <code>BenchResult</code>, treat it as "skip recording results"
     * 
     * @param name the name of the current section (ex. test name)
     */
    void setName(String name) {
        this.name = name;
    }

    /**
     * Collect a query that was run
     * 
     * @param query a Bench query
     */
    void logQuery(String query) {
        if (name == null)
            throw new RuntimeException("Set a test name before logging a query");
        if (isClosed)
            throw new RuntimeException("Attempted to log query to close Query Log");
        queries.add(query);
    }

    private void write(String text, int newLineCount) {
        try (BufferedWriter out = Files.newBufferedWriter(logFile, CREATE, APPEND)) {
            out.write(text);
            for (int i = 0; i < newLineCount; i++)
                out.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write to query log: " + logFile, ex);
        }
    }

    static Path getLogFile(Path parent, Class<?> testClass) {
        Path logFile = parent.resolve("test-logs/" + testClass.getName() + ".query.md");
        try {
            Files.createDirectories(logFile.getParent());
            return logFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create query log directory" + logFile.getParent(), ex);
        }
    }

}
