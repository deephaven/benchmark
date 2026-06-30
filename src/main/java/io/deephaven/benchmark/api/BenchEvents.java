/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.*;
import io.deephaven.benchmark.connect.ResultTable;

/**
 * Represents the events gathered during usage of the Bench API. These can include events gather by the API or the user.
 */
final public class BenchEvents {
    static final String header = "benchmark_name,origin,type,start,duration,name,value";
    final List<Event> events = new ArrayList<>();
    final Path file;
    private String name = null;

    BenchEvents(Path parent) {
        this(parent, Bench.eventsFileName);
    }

    BenchEvents(Path parent, String resultFileName) {
        this.file = parent.resolve(resultFileName);
    }

    /**
     * Add the results from a table as events to persist to the file system. This table must have columns defined as
     * <code>origin, type, start_ns, duration_ns, detail</code>
     * 
     * @param table a table containing events
     * @return this instance
     */
    public BenchEvents add(ResultTable table) {
        for (int r = 0, rn = table.getRowCount(); r < rn; r++) {
            var origin = table.getValue(r, "origin").toString();
            var type = table.getValue(r, "type").toString();
            var startNanos = table.getNumber(r, "start_ns").longValue();
            var durationNanos = table.getNumber(r, "duration_ns").longValue();
            var name = String.valueOf(table.getValue(r, "name"));
            var value = table.getNumber(r, "value").doubleValue();
            var event = new Event(origin, type, startNanos, durationNanos, name, value);
            events.add(event);
        }
        return this;
    }

    /**
     * Save the collected events to a csv file.
     */
    public void commit() {
        if (!hasHeader())
            writeLine(header, file);

        for (Event event : events) {
            var line = name + ',' + event.toCsv();
            writeLine(line, file);
        }
    }

    void setName(String name) {
        this.name = name;
    }

    private boolean hasHeader() {
        return Files.exists(file);
    }

    static void writeLine(String line, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write(line);
            out.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write result to file: " + file, ex);
        }
    }

    record Event(String origin, String type, long startNanos, long durationNanos, String name, double value) {
        String toCsv() {
            return origin + "," + type + "," + startNanos + "," + durationNanos + "," + name + "," + value;
        }
    }

}
