/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Duration;
import java.util.*;
import io.deephaven.benchmark.util.Log;
import io.deephaven.benchmark.util.Timer;

/**
 * Represents the results of a query instance. Results are collected for each test and appended to a CSV file when the
 * API is closed after each test. The results focus on rates for the test run.
 */
final public class BenchResult {
    static final String[] header = {"name", "timestamp", "duration", "test-rate", "test-row-count"};
    final Timer timer;
    final Map<String, Object> rates;
    final Path file;
    private String name = null;

    BenchResult(Path parent) {
        this(parent, Bench.resultFileName);
    }

    BenchResult(Path parent, String resultFileName) {
        this.file = parent.resolve(resultFileName);
        this.timer = Timer.start();
        this.rates = new LinkedHashMap<>();
        initializeRates(rates);
    }

    /**
     * Record a test rate for this result instance
     * 
     * @param timer a started timer measuring the test
     * @param count the processed item count (e.g. rowCount)
     * @return this result instance
     */
    public BenchResult test(Timer timer, long count) {
        rates.put("test-rate", new Rate(timer.duration(), count));
        rates.put("test-row-count", count);
        return this;
    }

    /**
     * Save the collected results a csv file. Skip results where name starts with "#"
     */
    public void commit() {
        if (name.startsWith("#"))
            return;

        List<String> head = Arrays.stream(header).toList();
        if (!hasHeader())
            writeLine(head, file);

        var m = new HashMap<String, Object>(rates);
        m.put("name", name);
        m.put("timestamp", timer.beginTime);
        m.put("duration", format(toSeconds(timer.duration())));
        Log.info("Result: %s", m);
        writeLine(head.stream().map(h -> m.get(h)).toList(), file);
        initializeRates(rates);
    }

    void setName(String name) {
        this.name = name;
    }

    private boolean hasHeader() {
        return Files.exists(file);
    }

    private void initializeRates(Map<String, Object> rates) {
        Arrays.stream(header).forEach(h -> rates.put(h, 0)); // Preserve key order
    }

    static void writeLine(Collection<?> values, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write(String.join(",", values.stream().map(v -> v.toString()).toList()));
            out.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write result to file: " + file, ex);
        }
    }

    static String format(float v) {
        return String.format("%.2f", v);
    }

    // Use toMillis() because toSeconds() loses the fraction
    static float toSeconds(Duration duration) {
        return duration.toMillis() / 1000.0f;
    }

    static record Rate(Duration duration, long rowCount) {
        @Override
        public String toString() {
            return format(rowCount / toSeconds(duration));
        }
    }

}
