/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.Closeable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Ids;
import io.deephaven.benchmark.util.Timer;

/**
 * The root accessor class for the API. Use <code>Bench.create(this)</code> in a typical JUnit test to start things off
 * <p/>
 * Bench API methods are not thread-safe, nor are they intended to be. It makes no sense to run benchmark tests in
 * parallel. If parallel tests are desired to shorten overall test time, use the standalone uber-jar and select separate
 * sets of test packages to run on different systems simultaneously.
 */
final public class Bench {
    static final public Path rootOutputDir = Paths.get("results");
    static final public String resultFileName = "benchmark-results.csv";
    static final public String metricsFileName = "benchmark-metrics.csv";
    static final public String platformFileName = "benchmark-platform.csv";
    static final Profile profile = new Profile();
    static final public Path outputDir = initializeOutputDirectory();

    static public Bench create(Object testInst) {
        Bench v = new Bench(testInst.getClass());
        v.setName(testInst.getClass().getSimpleName());
        return v;
    }

    final Object testInst;
    final BenchResult result;
    final BenchMetrics metrics;
    final BenchPlatform platform;
    final QueryLog queryLog;
    final BenchLog runLog;
    final List<Future<Metrics>> futures = new ArrayList<>();
    final List<Closeable> closeables = new ArrayList<>();
    private boolean isClosed = false;

    Bench(Class<?> testInst) {
        this.testInst = testInst;
        this.result = new BenchResult(outputDir);
        this.metrics = new BenchMetrics(outputDir);
        this.platform = new BenchPlatform(outputDir);
        this.queryLog = new QueryLog(outputDir, testInst);
        this.runLog = new BenchLog(outputDir, testInst);
    }

    /**
     * Set the name that identifies the currently running test. This name is used in logging and results
     * 
     * @param name the test name for reporting
     */
    public void setName(String name) {
        if (name == null || name.isBlank())
            throw new RuntimeException("No blank Benchmark names allowed");
        this.result.setName(name);
        this.metrics.setName(name);
        this.queryLog.setName(name);
        this.runLog.setName(name);
    }

    /**
     * Get a property from the profile, System, Environment or return a default value
     * 
     * @param name the property name
     * @param defaultValue value to return if the property does not exist
     * @return the property value or default
     */
    public String property(String name, String defaultValue) {
        return profile.property(name, defaultValue);
    }

    /**
     * Get an integral property from the profile, System, Environment or return a default value
     * 
     * @param name the property name
     * @param defaultValue value to return if the property does not exist
     * @return the property value or default
     */
    public long propertyAsIntegral(String name, String defaultValue) {
        return profile.propertyAsIntegral(name, defaultValue);
    }

    /**
     * Get a boolean property from the profile, System, Environment or return a default value
     * 
     * @param name the property name
     * @param defaultValue value <code>( true | false )</code> to return if the property does not exist
     * @return the property value or default
     */
    public boolean propertyAsBoolean(String name, String defaultValue) {
        return profile.propertyAsBoolean(name, defaultValue);
    }

    /**
     * Get an integral property from the profile, System, Environment or return a default value. Values are specified to
     * match the following regular expression:
     * <p/>
     * <code>[0-9]+ ( nanos | nano | millis | milli | seconds | second | minutes | minute )</code>
     * 
     * @param name the property name
     * @param defaultValue value to return if the property does not exist
     * @return the property value or default
     */
    public Duration propertyAsDuration(String name, String defaultValue) {
        return profile.propertyAsDuration(name, defaultValue);
    }

    /**
     * Start configuring a table
     * 
     * @param name the name of the table
     * @return a table configuration instance
     */
    public BenchTable table(String name) {
        return addCloseable(new BenchTable(this, name));
    }

    /**
     * Start configuring a query
     * 
     * @param logic the query logic that will be executed through a session
     * @return a query configuration instance
     */
    public BenchQuery query(String logic) {
        return addCloseable(new BenchQuery(this, logic, queryLog));
    }

    /**
     * Wait for all previously executed asynchronous tasks (e.g. generators, queries) to finish before moving on
     */
    public void awaitCompletion() {
        for (Future<Metrics> f : futures) {
            awaitCompletion(f);
        }
        futures.clear();
    }

    /**
     * Starts and returns a timer
     * 
     * @return a timer
     */
    public Timer timer() {
        return Timer.start();
    }

    /**
     * Get the result for this Benchmark instance (e.g. test) used for collecting rates
     * 
     * @return the result instance
     */
    public BenchResult result() {
        return result;
    }

    /**
     * Get the metrics for this Benchmark instance (e.g. test) used for collecting metric values
     * 
     * @return the metrics instance
     */
    public BenchMetrics metrics() {
        return metrics;
    }

    /**
     * Get the platform for this Benchmark instance (e.g. test) used for collecting platform properties
     * 
     * @return the platform instance
     */
    public BenchPlatform platform() {
        return platform;
    }

    /**
     * Get the metrics for this Benchmark instance (e.g. test) used for collecting metric values
     * 
     * @return the metrics instance
     */
    public BenchLog log() {
        return runLog;
    }

    /**
     * Has this Bench api instance been closed along with all connectors and files opened since creating the instance
     * 
     * @return true if already closed, otherwise false
     */
    public boolean isClosed() {
        return isClosed;
    }

    /**
     * Finish all running tasks (e.g. queries, generators), close any I/O, and append any results to the file system
     */
    public void close() {
        if (isClosed)
            return;
        isClosed = true;
        for (Closeable c : closeables) {
            try {
                c.close();
            } catch (Exception ex) {
                throw new RuntimeException("Failed to close: " + c.getClass().getName(), ex);
            }
        }
        closeables.clear();
        result.commit();
        metrics.commit();
        platform.commit();
        runLog.close();
        queryLog.close();
    }

    Metrics awaitCompletion(Future<Metrics> future) {
        try {
            long secs = propertyAsDuration("default.completion.timeout", "5 minutes").toSeconds();
            Metrics m = future.get(secs, TimeUnit.SECONDS);
            metrics.add(m);
            return m;
        } catch (Exception ex) {
            throw new RuntimeException("Timed out waiting for completion", ex);
        }
    }

    <T extends Closeable> T addCloseable(T closeable) {
        closeables.add(closeable);
        return closeable;
    }

    <T extends Future<Metrics>> T addFuture(T future) {
        futures.add(future);
        return future;
    }

    static private Path initializeOutputDirectory() {
        setSystemProperties();
        boolean isTimestamped = profile.propertyAsBoolean("timestamp.test.results", "false");
        Path dir = rootOutputDir;
        if (isTimestamped)
            dir = dir.resolve(Ids.runId());
        Filer.delete(dir);
        try {
            return Files.createDirectories(dir);
        } catch (Exception ex) {
            throw new RuntimeException("Failed initialize benchmark result directory: " + dir, ex);
        }
    }

    static private void setSystemProperties() {
        Duration timeout = profile.propertyAsDuration("default.completion.timeout", "5 minutes");
        System.setProperty("deephaven.session.executeTimeout", timeout.toString());

        if (!profile.isPropertyDefined("timestamp.test.results")) {
            System.setProperty("timestamp.test.results", "false");
        }
    }

}
