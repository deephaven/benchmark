package io.deephaven.benchmark.tests.standard.parquet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.util.Arrays;
import java.util.stream.Collectors;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Timer;

/**
 * Test reading and writing parquet files with various data types and compression codecs.
 */
class ParquetTestRunner {
    final Object testInst;
    final Bench api;
    private double rowCountFactor = 1;
    private int scaleFactor = 1;
    private long scaleRowCount;

    ParquetTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = initialize(testInst);
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
    }

    /**
     * Set a multiplier for the generated rows and a multiplier for simulating more rows with {@code merge}
     * 
     * @param rowCountFactor the multiplier for the scale.row.count property
     * @param scaleFactor the multiplier for how many merges to do on the generated table to simulate more rows
     */
    void setScaleFactors(double rowCountFactor, int scaleFactor) {
        this.rowCountFactor = rowCountFactor;
        this.scaleRowCount = (long) (api.propertyAsIntegral("scale.row.count", "100000") * rowCountFactor);
        this.scaleFactor = scaleFactor;
    }

    /**
     * Read a benchmark that measures parquet read performance. This tests always runs after a corresponding write test.
     * 
     * @param testName name that will appear in the results as the benchmark name
     */
    void runReadTest(String testName) {
        var q = """
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        source = read('/data/source.ptr.parquet').select()
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [source.size]),
            long_col("result_row_count", [source.size])
        ])
        """;
        runTest(testName, q);
    }

    /**
     * Run a benchmark the measures parquet write performance.
     * 
     * @param testName the benchmark name to record with the measurement
     * @param codec a compression codec
     * @param columnNames the names of the pre-defined columns to generate
     */
    void runWriteTest(String testName, String codec, String... columnNames) {
        var q = """
        source = merge([empty_table(${rowCount}).update([
            ${generators}
        ])] * ${scaleFactor})
        
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        write(
            source, '/data/source.ptr.parquet', compression_codec_name='${codec}', 
            max_dictionary_keys=2000000, max_dictionary_size=20000000, target_page_size=2000000
        )
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [source.size]),
            long_col("result_row_count", [source.size])
        ])
        """;
        q = q.replace("${rowCount}", "" + scaleRowCount);
        q = q.replace("${scaleFactor}", "" + scaleFactor);
        q = q.replace("${codec}", codec.equalsIgnoreCase("none") ? "UNCOMPRESSED" : codec);
        q = q.replace("${generators}", getGenerators(columnNames));
        runTest(testName, q);
    }

    /**
     * Run the test through barrage java client, collect the results, and report them.
     * 
     * @param testName the benchmark name to record with the results
     * @param query the test query to run
     */
    void runTest(String testName, String query) {
        try {
            api.setName(testName);
            api.query(query).fetchAfter("stats", table -> {
                long rowCount = table.getSum("processed_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                long resultRowCount = table.getSum("result_row_count").longValue();
                assertEquals(scaleRowCount * scaleFactor, resultRowCount);
                api.result().test("deephaven-engine", Duration.ofNanos(elapsedNanos), rowCount);
            }).fetchAfter("standard_metrics", table -> {
                api.metrics().add(table);
                var metrics = new Metrics(Timer.now(), "test-runner", "setup", "test");
                metrics.set("static_scale_factor", scaleFactor);
                metrics.set("row_count_factor", rowCountFactor);
                api.metrics().add(metrics);
            }).execute();
        } finally {
            api.close();
        }
    }

    /**
     * Get the lines of code required to generate the data for pre-defined column names
     * 
     * @param columnNames the column names to generate code for
     * @return the lines of code needed to generate column ndata
     */
    String getGenerators(String... columnNames) {
        return Arrays.stream(columnNames).map(c -> "'" + c + "=" + getGenerator(c) + "'")
                .collect(Collectors.joining(",\n")) + '\n';
    }

    /**
     * Get the code needed for generating data for the given pre-defined column name.
     * 
     * @param columnName the column name to generate data for
     * @return the data generation code
     */
    String getGenerator(final String columnName) {
        var array5 = "java.util.stream.IntStream.range((int)(ii % 5),(int)((ii % 5) + 5)).toArray()";
        var array1K = "java.util.stream.IntStream.range((int)(ii % 1000),(int)((ii % 1000) + 1000)).toArray()";
        var gen = switch (columnName) {
            case "str10K" -> "(`` + (ii % 10000))";
            case "long10K" -> "(ii % 10000)";
            case "int10K" -> "((int)(ii % 10000))";
            case "short10K" -> "((short)(ii % 10000))";
            case "bigDec10K" -> "java.math.BigDecimal.valueOf(ii % 10000)";
            case "array5" -> array5;
            case "vector5" -> "new io.deephaven.vector.IntVectorDirect(" + array5 + ")";
            case "array1K" -> array1K;
            case "vector1K" -> "new io.deephaven.vector.IntVectorDirect(" + array1K + ")";
            default -> throw new RuntimeException("Undefined column: " + columnName);
        };
        return "(ii % 10 == 0) ? null : " + gen;
    }

    /**
     * Initialize the test client and its properties. Restart Docker if it is local to the test client and the
     * {@code docker.compose.file} set.
     * 
     * @param testInst the test instance this runner is associated with.
     * @return a new Bench API instance.
     */
    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import empty_table, garbage_collect, new_table, merge
        from deephaven.column import long_col, double_col
        from deephaven.parquet import read, write
        """;

        Bench api = Bench.create(testInst);
        restartDocker(api);
        api.query(query).execute();
        return api;
    }

    /**
     * Restart Docker if it is local to this test runner and the {@code docker.compose.file} set.
     * 
     * @param api the Bench API for this test runner.
     */
    void restartDocker(Bench api) {
        var timer = api.timer();
        if (!Exec.restartDocker(api.property("docker.compose.file", ""), api.property("deephaven.addr", "")))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup", "docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

}
