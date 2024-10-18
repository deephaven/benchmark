/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.metric;

import java.util.*;

/**
 * Store metrics for a single origin, name and category. Since metrics may be collected from services, origin will
 * typically be the name of a services or host. Category is for grouping similar sets of metrics together. Name is for
 * identifying a set of metrics. All metrics in each instance have the same timestamp.
 */
public class Metrics {
    final Map<String, Metric> metrics = new TreeMap<>();
    final long timestamp;
    final String origin;
    final String category;

    /**
     * Initializes a newly created {@code Metrics} object representing a set of metrics that uses the current time for
     * timestamp
     * 
     * @param origin where the metrics came from (ex. myservice, myhost)
     * @param category groups these metrics with other metrics
     */
    public Metrics(String origin, String category) {
        this(System.currentTimeMillis(), origin, category);
    }

    /**
     * Initializes a newly created {@code Metrics} object representing a set of metrics
     * 
     * @param timestamp timestamp in millis since epoch the metric was taken
     * @param origin where the metrics came from (ex. myservice, myhost)
     * @param category groups these metrics with other metrics
     */
    public Metrics(long timestamp, String origin, String category) {
        this.timestamp = timestamp;
        this.origin = origin;
        this.category = category;
    }

    /**
     * Add a metric to this set of metrics, overwriting existing
     * 
     * @param metricName name of the metric
     * @param value numeric value of the metric
     * @param notes optional notes to clarify the metric
     * @return this instance
     */
    public Metrics set(String metricName, Number value, String... notes) {
        metrics.put(metricName, new Metric(metricName, value, String.join(";", notes)));
        return this;
    }

    /**
     * Get the value of a metric
     * 
     * @param metricName name of the metric
     * @return the value of the metric
     */
    public Number getValue(String metricName) {
        Metric m = metrics.get(metricName);
        return (m != null) ? m.value() : null;
    }

    /**
     * Get the note for a metric
     * 
     * @param metricName name of the metric
     * @return the note for the metric
     */
    public String getNote(String metricName) {
        Metric m = metrics.get(metricName);
        return (m != null) ? m.note() : null;
    }

    /**
     * Get a metric Map with the given name
     * 
     * @param metricName the name of the metric to retrieve
     * @return a <code>Map</code> containing the metric details
     */
    public Map<String, Object> getMetric(String metricName) {
        var v = getValue(metricName);
        var n = getNote(metricName);
        if (v == null || n == null)
            return null;

        return Map.of("timestamp", timestamp, "origin", origin, "name", category + '.' + metricName, "value", v,
                "note", n);
    }

    /**
     * Get the timestamp in millis since epoch for this set of metrics
     * 
     * @return timestamp since epoch
     */
    public long timestamp() {
        return timestamp;
    }

    /**
     * Get the metric names contained in this set
     * 
     * @return the metric names
     */
    public Set<String> getNames() {
        return Collections.unmodifiableSet(metrics.keySet());
    }

    /**
     * Provide a description of the metrics suitable for printing one line to the console
     */
    @Override
    public String toString() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("timestamp", timestamp);
        m.put("origin", origin);
        m.put("category", category);
        m.putAll(metrics);
        return m.toString();
    }

    record Metric(String metricName, Number value, String note) {
        @Override
        public String toString() {
            return value + (note.isBlank() ? "" : (";" + note));
        }
    }

}
