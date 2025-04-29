package io.deephaven.benchmark.tests.standard.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.controller.Controller;
import io.deephaven.benchmark.controller.DeephavenDockerController;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Timer;

/**
 * Test reading and writing parquet files with various data types and compression codecs.
 */
class FileTestRunner {
    final String parquetCfg = "max_dictionary_keys=1048576, max_dictionary_size=1048576, target_page_size=65536";
    final Object testInst;
    final Set<String> requiredServices = new TreeSet<>(List.of("deephaven"));
    private Bench api;
    private Controller controller;
    private double rowCountFactor = 1;
    private int scaleFactor = 1;
    private long scaleRowCount;
    private boolean useParquetDefaultSettings = false;

    FileTestRunner(Object testInst) {
        this.testInst = testInst;
        initialize(testInst);
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
     * Sets the prefixes of the services required to run this test. Those services will be turned on while the rest will
     * be turned off.
     * 
     * @param services the services to run the test
     */
    void setRequiredServices(String... servicePrefixes) {
        requiredServices.clear();
        requiredServices.addAll(Arrays.asList(servicePrefixes));
    }

    /**
     * Use the default settings in deephaven-core for parquet dictionary and page size instead of the defaults used for
     * benchmarks
     */
    void useParquetDefaultSettings() {
        this.useParquetDefaultSettings = true;
    }

    /**
     * Run a benchmark that measures csv read performance. This test always runs after a corresponding write test.
     * 
     * @param testName name that will appear in the results as the benchmark name
     */
    void runCsvReadTest(String testName, String... columnNames) {
        var q = """ 
        read_csv('/data/source.ptr.csv', ${types})
        metric_file_size = os.path.getsize('/data/source.ptr.csv')
        bench_api_metrics_add('data', 'file.size', str(metric_file_size), 'csv')
        """;
        q = q.replace("${types}", getTypes(columnNames));
        runReadTest(testName, q);
    }

    /**
     * Run a benchmark that measures parquet read performance. This test always runs after a corresponding write test.
     * 
     * @param testName name that will appear in the results as the benchmark name
     */
    void runParquetReadTest(String testName) {
        var q = """
        read('/data/source.ptr.parquet').select()
        metric_file_size = os.path.getsize('/data/source.ptr.parquet')
        bench_api_metrics_add('data', 'file.size', str(metric_file_size), 'parquet')
        """;
        runReadTest(testName, q);
    }

    /**
     * Run a benchmark that measures parquet write performance.
     * 
     * @param testName the benchmark name to record with the measurement
     * @param codec a compression codec
     * @param columnNames the names of the pre-defined columns to generate
     */
    void runParquetWriteTest(String testName, String codec, String... columnNames) {
        var q = """
        write(
            source, '/data/source.ptr.parquet', compression_codec_name='${codec}'${parquetSettings}
        )
        metric_file_size = os.path.getsize('/data/source.ptr.parquet')
        bench_api_metrics_add('data', 'file.size', str(metric_file_size), 'parquet')
        """;
        q = q.replace("${codec}", codec.equalsIgnoreCase("none") ? "UNCOMPRESSED" : codec);
        q = q.replace("${parquetSettings}", useParquetDefaultSettings ? "" : (",\n    " + parquetCfg));
        runWriteTest(testName, q, columnNames);
    }

    /**
     * Run a benchmark the measures csv write performance.
     * 
     * @param testName the benchmark name to record with the measurement
     * @param columnNames the names of the pre-defined columns to generate
     */
    void runCsvWriteTest(String testName, String... columnNames) {
        var q = """
        write_csv(source, '/data/source.ptr.csv')
        metric_file_size = os.path.getsize('/data/source.ptr.csv')
        bench_api_metrics_add('data', 'file.size', str(metric_file_size), 'csv')
        """;
        runWriteTest(testName, q, columnNames);
    }

    /**
     * Run a benchmark that measures read performance. This test always runs after a corresponding write test.
     * 
     * @param testName name that will appear in the results as the benchmark name
     */
    void runReadTest(String testName, String readQuery, String... columnNames) {
        var q = """
        bench_api_metrics_start()
        begin_time = time.perf_counter_ns()
        source = ${readQuery}
        end_time = time.perf_counter_ns()
        bench_api_metrics_end()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [source.size]),
            long_col("result_row_count", [source.size])
        ])
        """;
        q = q.replace("${readQuery}", readQuery);
        runTest(testName, q);
    }

    void runWriteTest(String testName, String writeQuery, String... columnNames) {
        cleanWritePaths(api);
        var q = """
        if(${scaleFactor} > 1):
            source = merge([empty_table(${rowCount}).update([
                ${generators}
            ])] * ${scaleFactor})
        else:
            source = empty_table(${rowCount}).update([${generators}])
        
        sourceSize = source.size
        
        bench_api_metrics_start()
        begin_time = time.perf_counter_ns()
        ${writeQuery}
        end_time = time.perf_counter_ns()
        bench_api_metrics_end()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [sourceSize]),
            long_col("result_row_count", [sourceSize])
        ])
        """;
        q = q.replace("${writeQuery}", writeQuery);
        q = q.replace("${rowCount}", "" + scaleRowCount);
        q = q.replace("${scaleFactor}", "" + scaleFactor);
        q = q.replace("${generators}", getGenerators(columnNames));
        runTest(testName, q);
    }

    private void runTest(String testName, String query) {
        try {
            api.setName(testName);
            stopUnusedServices(requiredServices);
            api.query(query).fetchAfter("stats", table -> {
                long rowCount = table.getSum("processed_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                long resultRowCount = table.getSum("result_row_count").longValue();
                assertEquals(scaleRowCount * scaleFactor, resultRowCount);
                api.result().test("deephaven-engine", Duration.ofNanos(elapsedNanos), rowCount);
            }).fetchAfter("standard_metrics", table -> {
                api.metrics().add(table);
                var metrics = new Metrics(Timer.now(), "test-runner", "setup.scale");
                metrics.set("static_scale_factor", scaleFactor);
                metrics.set("row_count_factor", rowCountFactor);
                api.metrics().add(metrics);
            }).execute();
        } finally {
            addDockerLog(api);
            api.close();
        }
    }

    /**
     * Get the lines of code required to generate the data for pre-defined column names
     * 
     * @param columnNames the column names to generate code for
     * @return the lines of code needed to generate column ndata
     */
    private String getGenerators(String... columnNames) {
        return Arrays.stream(columnNames).map(c -> "'" + c + "=" + getGenerator(c) + "'")
                .collect(Collectors.joining(",\n")) + '\n';
    }

    /**
     * Get the code needed for generating data for the given pre-defined column name.
     * 
     * @param columnName the column name to generate data for
     * @return the data generation code
     */
    private String getGenerator(final String columnName) {
        var array5 = "java.util.stream.IntStream.range((int)(ii % 5),(int)((ii % 5) + 5)).toArray()";
        var array1K = "java.util.stream.IntStream.range((int)(ii % 1000),(int)((ii % 1000) + 1000)).toArray()";
        var objArr5 = "java.util.stream.Stream.of(`1`,null,`3`,null,`5`).toArray()";
        var gen = switch (columnName) {
            case "str10K" -> "(`` + (ii % 10000))";
            case "long10K" -> "(ii % 10000)";
            case "int10K" -> "((int)(ii % 10000))";
            case "short10K" -> "((short)(ii % 10000))";
            case "byte100" -> "((byte)(ii % 100))";
            case "bigDec10K" -> "java.math.BigDecimal.valueOf(ii % 10000)";
            case "intArr5" -> array5;
            case "intVec5" -> "vec(" + array5 + ")";
            case "intArr1K" -> array1K;
            case "intVec1K" -> "vec(" + array1K + ")";
            case "objArr5" -> objArr5;
            case "objVec5" -> "vecObj(" + objArr5 + ")";
            default -> throw new RuntimeException("Undefined column: " + columnName);
        };
        return "(ii % 10 == 0) ? null : " + gen;
    }

    private String getTypes(String... cols) {
        return "{" + Arrays.stream(cols).map(c -> "'" + c + "':" + getType(c)).collect(Collectors.joining(",")) + "}";
    }

    private String getType(String columnName) {
        return switch (columnName) {
            case "str10K" -> "dht.string";
            case "long10K" -> "dht.long";
            case "int10K" -> "dht.int32";
            case "short10K" -> "dht.short";
            case "byte100" -> "dht.byte";
            case "bigDec10K" -> "dht.BigDecimal";
            case "intArr5" -> "dht.int_array";
            case "intVec5" -> "dht.int_array";
            case "intArr1K" -> "dht.int_array";
            case "intVec1K" -> "dht.int_array";
            case "objArr5" -> "string_array";
            case "objVec5" -> "string_array";
            default -> throw new RuntimeException("Undefined column: " + columnName);
        };
    }

    private Bench initialize(Object testInst) {
        var query = """
        import time, os, shutil
        from deephaven import empty_table, garbage_collect, new_table, merge
        from deephaven.column import long_col, double_col
        from deephaven.parquet import read, write, write_partitioned
        from deephaven import read_csv, write_csv
        from deephaven import dtypes as dht
        from deephaven.experimental import s3
        
        def remove_path(path):
            if(os.path.exists(path)):
                if(os.path.isdir(path)): shutil.rmtree(path)
                else: os.remove(path)
        
        bench_api_metrics_init()
        """;

        this.api = Bench.create(testInst);
        this.controller = new DeephavenDockerController(api.property("docker.compose.file", ""),
                api.property("deephaven.addr", ""));
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
        restartDocker();
        api.query(query).execute();
        return api;
    }

    private void addDockerLog(Bench api) {
        var timer = api.timer();
        var logText = controller.getLog();
        if (logText.isBlank())
            return;
        api.log().add("deephaven-engine", logText);
        var metrics = new Metrics(Timer.now(), "test-runner", "teardown.docker");
        metrics.set("log", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    private void restartDocker() {
        var timer = api.timer();
        if (!controller.restartService())
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }
    
    private void stopUnusedServices(Set<String> keepServices) {
        var timer = api.timer();
        if (!controller.stopService(keepServices))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.services");
        metrics.set("stop", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }
    
    private void cleanWritePaths(Bench api) {
        try {
            var q = """
            remove_path('/data/source.ptr.parquet')
            remove_path('/data/source.ptr.csv')
            remove_path('/minio/data/source.partitioned')
            """;
            api.query(q).execute();
        } catch(Exception ex) {
            throw new RuntimeException("Error cleaning up CSV and Parquet files", ex);
        }
    }

}
