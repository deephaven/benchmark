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
    static final String[] header =
            {"benchmark_name", "origin", "timestamp", "test_duration", "op_duration", "op_rate", "row_count"};
    final Timer timer;
    final Map<String, Object> rate;
    final Path file;
    private String name = null;

    BenchResult(Path parent) {
        this(parent, Bench.resultFileName);
    }

    BenchResult(Path parent, String resultFileName) {
        this.file = parent.resolve(resultFileName);
        this.timer = Timer.start();
        this.rate = new LinkedHashMap<>();
        initializeRates(rate);
    }

    /**
     * Record a test rate for this result instance
     * 
     * @param the place where the measurement was collected
     * @param timer a started timer measuring the test
     * @param count the processed item count (e.g. rowCount)
     * @return this result instance
     */
    public BenchResult test(String origin, Timer timer, long count) {
        test(origin, timer.duration(), count);
        return this;
    }

    /**
     * Record a test rate for this result instance
     * 
     * @param origin the place where the measurement was collected
     * @param timer a started timer measuring the test
     * @param count the processed item count (e.g. rowCount)
     * @return this result instance
     */
    public BenchResult test(String origin, Duration duration, long count) {
        rate.put("origin", origin);
        rate.put("op_duration", duration);
        rate.put("row_count", count);
        return this;
    }

    /**
     * Save the collected results to a csv file. Skip results where name starts with "#". If no user-specified test
     * result has been supplied, calculate add a default based on time since test start
     */
    public void commit() {
        if (name.startsWith("#"))
            return;

        List<String> head = Arrays.stream(header).toList();
        if (!hasHeader())
            writeLine(head, file);

        ensureTestRate();

        var m = new HashMap<String, Object>(rate);
        m.put("benchmark_name", name);
        m.put("timestamp", timer.beginTime);
        m.put("test_duration", format(toSeconds(timer.duration())));
        m.put("op_rate", toRate(m.get("op_duration"), m.get("row_count")));
        m.put("op_duration", format(toSeconds((Duration) m.get("op_duration"))));
        Log.info("Result: %s", m);
        writeLine(head.stream().map(h -> m.get(h)).toList(), file);
        initializeRates(rate);
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

    private void ensureTestRate() {
        if (!rate.get("row_count").equals(0))
            return;
        test("n/a", timer, Bench.profile.propertyAsIntegral("scale.row.count", "10000"));
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

    static long toRate(Object duration, Object count) {
        return (long) (((Number) count).longValue() / toSeconds((Duration) duration));
    }

}
