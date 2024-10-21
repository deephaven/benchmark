/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import static java.nio.file.StandardOpenOption.*;
import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Contains log data from the target where the benchmarks are run (e.g. Deephaven Engine). There is one log per test
 * Class, and test name markers are used as beginning and end of the log lines for each benchmark in the log file.
 */
final public class BenchLog {
    final Class<?> testClass;
    final Path parent;
    final Path logFile;
    private String name = null;
    private boolean isClosed = false;

    /**
     * Initialize the log according to the test class it's tracking
     * 
     * @param parent the parent directory of the log
     * @param testClass the test class instance being tracked
     */
    BenchLog(Path parent, Class<?> testClass) {
        this.testClass = testClass;
        this.parent = parent;
        this.logFile = getLogFile(parent, testClass);
    }

    /**
     * Mark the end of the run log for the currents test class
     */
    public void close() {
        if (isClosed)
            return;
        isClosed = true;
    }

    /**
     * Add the log info that was collected during the test run
     * 
     * @param origin where the log message came from
     * @param info the log info (i.e. the docker log)
     */
    public void add(String origin, String info) {
        if (name == null)
            throw new RuntimeException("Set a test name before logging a test run");
        if (isClosed)
            throw new RuntimeException("Attempted to log to closed log");
        write("\n<<<<< BENCH_TEST," + origin + "," + name + ",BENCH_TEST >>>>>\n\n" + info.trim() + '\n');
    }

    /**
     * Set the name of the current test. This will be used at the beginning and end of the test's log info.
     * <p>
     * Note: The symbol "#" is used is some components like <code>QueryLog</code> to treat with special behavior. In
     * this log it is removed and treated like any other test heading.
     * 
     * @param name the name of the current log section (i.e. test name)
     */
    void setName(String name) {
        this.name = name;
    }

    private void write(String text) {
        try (BufferedWriter out = Files.newBufferedWriter(logFile, CREATE, APPEND)) {
            out.write(text);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write to the run log: " + logFile, ex);
        }
    }

    static Path getLogFile(Path parent, Class<?> testClass) {
        Path logFile = parent.resolve("test-logs/" + testClass.getName() + ".run.log");
        try {
            Files.createDirectories(logFile.getParent());
            return logFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create run log directory" + logFile.getParent(), ex);
        }
    }

}
