/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Timer;

/**
 * A wrapper for the Bench api that allows the running of small (single-operation) tests without requiring the
 * boilerplate logic like imports, parquet reads, time measurement logic, etc. Each <code>test</code> runs two tests;
 * one reading from a static parquet, and the other exercising ticking tables through the
 * <code>AutotuningIncrementalReleaseFilter</code>. Note: This class is meant to keep the majority of single-operations
 * compact and readable, not to cover every possible case. Standard query API code can be used in conjunction as long as
 * conventions are followed (ex. main file is "source")
 */
public class CompareTestRunner {
    final Object testInst;
    final List<String> setupQueries = new ArrayList<>();
    private String mainTable = "source";
    private Bench api;
    private long scaleRowCount;

    public CompareTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = initialize(testInst);
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
    }

    /**
     * Get the Bench API instance for this runner
     * 
     * @return the Bench API instance
     */
    public Bench api() {
        return api;
    }

    /**
     * Identify a pre-defined table for use by this runner
     * 
     * @param type
     */
    public void tables(String... names) {
        if (names.length > 0)
            mainTable = names[0];

        for (String name : names) {
            generateTable(name, null);
        }
    }

    /**
     * Add a query to be run outside the benchmark measurement but before the benchmark query. This query can transform
     * the main table or supporting table, set up aggregations or updateby operations, etc.
     * 
     * @param query the query to run before benchmark
     */
    public void addSetupQuery(String query) {
        setupQueries.add(query);
    }
    
    /**
     * The {@code scale.row.count} property supplies a default for the number of rows generated for benchmark tests.
     * Given that some operations use less memory than others, scaling up the generated rows per operation is more
     * effective than using scale factors {@link #setScaleFactors(int, int)}.
     * 
     * @param rowCountFactor a multiplier applied against {@code scale.row.count}
     */
    public void setRowFactor(int rowCountFactor) {
        this.scaleRowCount = (long) (api.propertyAsIntegral("scale.row.count", "100000") * rowCountFactor);
    }

    public void test(String name, String operation, String mainSizeGetter, String resultSizeGetter) {
        test(name, 0, operation, mainSizeGetter, resultSizeGetter);
    }

    public void test(String name, long expectedRowCount, String operation, String mainSizeGetter, String resultSizeGetter) {
        var result = runStaticTest(name, operation, mainSizeGetter, resultSizeGetter);
        var rcount = result.resultRowCount();
        var ecount = getExpectedRowCount(expectedRowCount);
        assertTrue(rcount > 0 && rcount <= ecount, "Wrong result row count: " + rcount);
    }

    long getExpectedRowCount(long expectedRowCount) {
        return (expectedRowCount < 1) ? Long.MAX_VALUE : expectedRowCount;
    }

    Result runStaticTest(String name, String operation, String mainSizeGetter, String resultSizeGetter) {
        var staticQuery = """
        ${setupQueries}

        begin_time = time.perf_counter_ns()
        result = ${operation}
        end_time = time.perf_counter_ns()
        main_size = ${mainSizeGetter}
        result_size = ${resultSizeGetter}
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [main_size]),
            long_col("result_row_count", [result_size]),
        ])
        """;
        return runTest(name + " -Static", staticQuery, operation, mainSizeGetter, resultSizeGetter);
    }

    Result runTest(String name, String query, String operation, String mainSizeGetter, String resultSizeGetter) {
        if (api.isClosed())
            api = initialize(testInst);
        api.setName(name);
        query = query.replace("${mainTable}", mainTable);
        query = query.replace("${setupQueries}", String.join("\n", setupQueries));
        query = query.replace("${operation}", operation);
        query = query.replace("${mainSizeGetter}", mainSizeGetter);
        query = query.replace("${resultSizeGetter}", resultSizeGetter);

        try {
            var result = new AtomicReference<Result>();
            api.query(query).fetchAfter("stats", table -> {
                long loadedRowCount = table.getSum("processed_row_count").longValue();
                long resultRowCount = table.getSum("result_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                var r = new Result(loadedRowCount, Duration.ofNanos(elapsedNanos), resultRowCount);
                result.set(r);
            }).execute();
            api.result().test("deephaven-engine", result.get().elapsedTime(), result.get().loadedRowCount());
            return result.get();
        } finally {
            api.close();
        }
    }

    String listStr(String... values) {
        return String.join(", ", Arrays.stream(values).map(c -> "'" + c + "'").toList());
    }

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import new_table, garbage_collect, merge
        from deephaven.column import long_col, double_col
        from deephaven.parquet import read
        """;

        Bench api = Bench.create(testInst);
        restartDocker(api);
        api.query(query).execute();
        return api;
    }

    void restartDocker(Bench api) {
        var timer = api.timer();
        if (!Exec.restartDocker(api.property("docker.compose.file", ""), api.property("deephaven.addr", "")))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup", "docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    void generateTable(String name, String distribution) {
        switch (name) {
            case "source":
                generateSourceTable(distribution);
                break;
            case "right":
                generateRightTable(distribution);
                break;
            case "timed":
                generateTimedTable(distribution);
                break;
            default:
                throw new RuntimeException("Undefined table name: " + name);
        }
    }

    void generateSourceTable(String distribution) {
        api.table("source")
                .add("int250", "int", "[1-250]", distribution)
                .add("int640", "int", "[1-640]", distribution)
                .add("int1M", "int", "[1-1000000]", distribution)
                .add("float5", "float", "[1-5]", distribution)
                .add("str250", "string", "[1-250]", distribution)
                .add("str640", "string", "[1-640]", distribution)
                .add("str1M", "string", "[1-1000000]", distribution)
                .withCompression("snappy")
                .withRowCount(scaleRowCount)
                .generateParquet();
    }

    void generateRightTable(String distribution) {
        api.table("right").fixed()
                .add("r_str250", "string", "[1-250]", distribution)
                .add("r_str640", "string", "[1-640]", distribution)
                .add("r_int1M", "int", "[1-1000000]", distribution)
                .add("r_str1M", "string", "[1-1000000]", distribution)
                .add("r_str10K", "string", "[1-100000]", distribution)
                .withCompression("snappy")
                .generateParquet();
    }

    void generateTimedTable(String distribution) {
        long baseTime = 1676557157537L;
        api.table("timed").fixed()
                .add("timestamp", "timestamp-millis", "[" + baseTime + "-" + (baseTime + scaleRowCount - 1) + "]")
                .add("int5", "int", "[1-5]", distribution)
                .add("int10", "int", "[1-10]", distribution)
                .add("float5", "float", "[1-5]", distribution)
                .add("str100", "string", "[1-100]", distribution)
                .add("str150", "string", "[1-150]", distribution)
                .withCompression("snappy")
                .generateParquet();
    }

    record Result(long loadedRowCount, Duration elapsedTime, long resultRowCount) {
    }

}
