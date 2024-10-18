/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.controller.Controller;
import io.deephaven.benchmark.controller.DeephavenDockerController;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Timer;

/**
 * A wrapper for the Bench api that allows the running of small (single-operation) tests without requiring the
 * boilerplate logic like imports, parquet reads, time measurement logic, etc. Each <code>test</code> runs two tests;
 * one reading from a static parquet, and the other exercising ticking tables through the
 * <code>AutotuningIncrementalReleaseFilter</code>. Note: This class is meant to keep the majority of single-operations
 * compact and readable, not to cover every possible case. Standard query API code can be used in conjunction as long as
 * conventions are followed (ex. main file is "source")
 */
final public class StandardTestRunner {
    final Object testInst;
    final List<String> supportTables = new ArrayList<>();
    final List<String> setupQueries = new ArrayList<>();
    final List<String> preOpQueries = new ArrayList<>();
    final Set<String> requiredServices = new TreeSet<>(List.of("deephaven"));
    private String mainTable = "source";
    private Bench api;
    private Controller controller;
    private int staticFactor = 1;
    private int incFactor = 1;
    private int rowCountFactor = 1;

    public StandardTestRunner(Object testInst) {
        this.testInst = testInst;
        initialize(testInst);
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
     * Generate the given pre-defined tables according to the default data distribution defined by the
     * <code>default.data.distribution</code> property. The first table name provided will be the main
     * <code>source</code> table.
     * <p>
     * This method should only be called once per test.
     * 
     * @param names the table names
     */
    public void tables(String... names) {
        if (names.length > 0)
            mainTable = names[0];

        for (String name : names) {
            generateTable(name, null, null);
        }
    }

    /**
     * Generate a pre-defined table and sets an explicit distribution for that table's data. This will override the
     * <code>default.data.distribution</code> property. The given table name will be used as the main table used by
     * subsequent queries.
     * 
     * @param name the table name to generate
     * @param distribution the name of the distribution (random | runlength | ascending | descending)
     */
    public void table(String name, String distribution) {
        mainTable = name;
        generateTable(name, distribution, null);
    }

    /**
     * Generate a pre-defined table and set a column grouping for the resulting table. The given table name will be used
     * as the main table used by subsequent queries.
     * <p>
     * 
     * @param name the table name to generate
     * @param groups
     */
    public void groupedTable(String name, String... groups) {
        mainTable = name;
        generateTable(name, null, groups);
    }
    
    public void setServices(String... services) {
        requiredServices.clear();
        requiredServices.addAll(Arrays.asList(services));
    }

    /**
     * Add a query to be run directly after the main table is loaded. It is not measured. This query can transform the
     * main table or supporting table, set up aggregations or updateby operations, etc.
     * 
     * @param query the query to run before benchmark
     */
    public void addSetupQuery(String query) {
        setupQueries.add(query);
    }

    /**
     * Add a query to be run directly before the measured operation is run. This query allows changes to tables or
     * config that must occur after other setup queries happen but before the operation is run. When in doubt, use
     * <code>addSetupQuery</code>.
     * 
     * @param query the query to run just before the measured operation
     */
    public void addPreOpQuery(String query) {
        preOpQueries.add(query);
    }

    /**
     * The {@code scale.row.count} property supplies a default for the number of rows generated for benchmark tests.
     * Given that some operations use less memory than others, scaling up the generated rows per operation is more
     * effective than using scale factors {@link #setScaleFactors(int, int)}.
     * 
     * @param rowCountFactor a multiplier applied against {@code scale.row.count}
     */
    public void setRowFactor(int rowCountFactor) {
        this.rowCountFactor = rowCountFactor;
    }

    /**
     * Scale static and incremental tests row counts artificially using Deephaven merge operations to avoid the added
     * cost of loading fully generated data into memory.
     * 
     * Simulate a higher row count by specifying a multiplier (Factor) for the row count. For example, if
     * scale.row.count=10, and staticFactor=2, the resulting row count used for the static test and rate will be 20
     * 
     * @param staticFactor the multiplier used to "merge-amplify" rows for the static test
     * @param incFactor the multiplier used to "merge-amplify" rows for the inc test
     */
    public void setScaleFactors(int staticFactor, int incFactor) {
        this.staticFactor = staticFactor;
        this.incFactor = incFactor;
    }

    /**
     * Run a single operation test through the Bench API with no upper bound expected on the resulting row count
     * 
     * @see #test(String, long, String, String...)
     * @param name the name of the test as it will show in the result file
     * @param operation the operation to run and measure for the benchmark
     * @param loadColumns columns to load from the generated parquet file
     */
    public void test(String name, String operation, String... loadColumns) {
        test(name, 0, operation, loadColumns);
    }

    /**
     * Run a single-operation test through the Bench API. Create and Bench instance if necessary. Run according to the
     * following contract:
     * <ul>
     * <li>Run test from static parquet read</li>
     * <li>If static test duration <code>&lt; scale.elapsed.time.target</code>, scale row count and do it again</li>
     * <li>Run test with auto increment release filter according to the previously determined row count</li>
     * <li>Assert that both static and incremental result tables have the same number of rows</li>
     * <p>
     * 
     * @param name the name of the test as it will show in the result file
     * @param expectedRowCount the max row count expected from the operation regardless of scale, or zero if the count
     *        has no upper bound
     * @param operation the operation to run and measure for the benchmark
     * @param loadColumns columns to load from the generated parquet file
     */
    public void test(String name, long maxExpectedRowCount, String operation, String... loadColumns) {
        if (staticFactor > 0) {
            var read = getReadOperation(staticFactor, loadColumns);
            var result = runStaticTest(name, operation, read, loadColumns);
            var rcount = result.resultRowCount();
            var ecount = getMaxExpectedRowCount(maxExpectedRowCount, staticFactor);
            assertTrue(rcount > 0 && rcount <= ecount, "Wrong result Static row count: " + rcount);
        }

        if (incFactor > 0) {
            var read = getReadOperation(incFactor, loadColumns);
            var result = runIncTest(name, operation, read, loadColumns);
            var rcount = result.resultRowCount();
            var ecount = getMaxExpectedRowCount(maxExpectedRowCount, incFactor);
            assertTrue(rcount > 0 && rcount <= ecount, "Wrong result Inc row count: " + rcount);
        }
    }

    long getGeneratedRowCount() {
        return (long) (api.propertyAsIntegral("scale.row.count", "100000") * rowCountFactor);
    }

    long getMaxExpectedRowCount(long expectedRowCount, long scaleFactor) {
        return (expectedRowCount < 1) ? Long.MAX_VALUE : expectedRowCount;
    }

    String getReadOperation(int scaleFactor, String... loadColumns) {
        if (scaleFactor > 1 && mainTable.equals("timed") && Arrays.asList(loadColumns).contains("timestamp")) {
            var read = """
            merge([
                read('/data/timed.parquet').view(formulas=[${loadColumns}])
            ] * ${scaleFactor}).update_view([
                'timestamp=timestamp.plusMillis((long)(ii / ${rows}) * ${rows})'
            ]).select()
            """;
            return read.replace("${scaleFactor}", "" + scaleFactor).replace("${rows}", "" + getGeneratedRowCount());
        }

        var read = "read('/data/${mainTable}.parquet').select(formulas=[${loadColumns}])";
        read = (loadColumns.length == 0) ? ("empty_table(" + getGeneratedRowCount() + ")") : read;

        if (scaleFactor > 1) {
            read = "merge([${readTable}] * ${scaleFactor})".replace("${readTable}", read);
            read = read.replace("${scaleFactor}", "" + scaleFactor);
        }
        return read;
    }

    Result runStaticTest(String name, String operation, String read, String... loadColumns) {
        var staticQuery = """
        ${loadSupportTables}
        ${mainTable} = ${readTable}
        loaded_tbl_size = ${mainTable}.size
        ${setupQueries}

        garbage_collect()

        ${preOpQueries}
        print('${logOperationBegin}')
        
        begin_time = time.perf_counter_ns()
        result = ${operation}
        end_time = time.perf_counter_ns()
        print('${logOperationEnd}')
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [loaded_tbl_size]),
            long_col("result_row_count", [result.size]),
        ])
        """;
        return runTest(name + " -Static", staticQuery, operation, read, loadColumns);
    }

    Result runIncTest(String name, String operation, String read, String... loadColumns) {
        var incQuery = """
        source = right = timed = None
        ${loadSupportTables}
        ${mainTable} = ${readTable}
        loaded_tbl_size = ${mainTable}.size
        ${setupQueries}
        
        autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
        source_filter = autotune(0, 1000000, 1.0, True)
        ${mainTable} = ${mainTable}.where(source_filter)
        if right: 
            right_filter = autotune(0, 1010000, 1.0, True)
            right = right.where(right_filter)
            print('Using Inc Right')
        
        garbage_collect()
        
        ${preOpQueries}
        print('${logOperationBegin}')
        begin_time = time.perf_counter_ns()
        result = ${operation}
        
        if right: right_filter.start()
        source_filter.start()
        
        from deephaven.execution_context import get_exec_ctx
        get_exec_ctx().update_graph.j_update_graph.requestRefresh()
        
        if right: right_filter.waitForCompletion()
        source_filter.waitForCompletion()
        
        end_time = time.perf_counter_ns()
        print('${logOperationEnd}')
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [loaded_tbl_size]),
            long_col("result_row_count", [result.size])
        ])
        """;
        return runTest(name + " -Inc", incQuery, operation, read, loadColumns);
    }

    Result runTest(String name, String query, String operation, String read, String... loadColumns) {
        if (api.isClosed())
            initialize(testInst);
        api.setName(name);
        stopUnusedServices(requiredServices);
        
        query = query.replace("${readTable}", read);
        query = query.replace("${mainTable}", mainTable);
        query = query.replace("${loadSupportTables}", loadSupportTables());
        query = query.replace("${loadColumns}", listStr(loadColumns));
        query = query.replace("${setupQueries}", String.join("\n", setupQueries));
        query = query.replace("${preOpQueries}", String.join("\n", preOpQueries));
        query = query.replace("${operation}", operation);
        query = query.replace("${logOperationBegin}", getLogSnippet("Begin", name));
        query = query.replace("${logOperationEnd}", getLogSnippet("End", name));

        try {
            var result = new AtomicReference<Result>();
            api.query(query).fetchAfter("stats", table -> {
                long loadedRowCount = table.getSum("processed_row_count").longValue();
                long resultRowCount = table.getSum("result_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                var r = new Result(loadedRowCount, Duration.ofNanos(elapsedNanos), resultRowCount);
                result.set(r);
            }).fetchAfter("standard_metrics", table -> {
                api.metrics().add(table);
                var metrics = new Metrics(Timer.now(), "test-runner", "setup.scale");
                metrics.set("static_scale_factor", staticFactor);
                metrics.set("inc_scale_factor", incFactor);
                metrics.set("row_count_factor", rowCountFactor);
                api.metrics().add(metrics);
            }).execute();
            api.result().test("deephaven-engine", result.get().elapsedTime(), result.get().loadedRowCount());
            return result.get();
        } finally {
            addServiceLog(api);
            api.close();
        }
    }

    String listStr(String... values) {
        return String.join(", ", Arrays.stream(values).map(c -> "'" + c + "'").toList());
    }

    String loadSupportTables() {
        return supportTables.stream().map(t -> t + " = read('/data/" + t + ".parquet').select()\n")
                .collect(Collectors.joining(""));
    }

    String getLogSnippet(String beginEnd, String name) {
        beginEnd = "BENCH_OPERATION_" + beginEnd.toUpperCase();
        return String.join(",", "<<<<< " + beginEnd, name, beginEnd + " >>>>>");
    }

    void initialize(Object testInst) {
        var query = """
        import time, jpy
        from deephaven import new_table, empty_table, garbage_collect, merge 
        from deephaven.column import long_col, double_col
        from deephaven.parquet import read
        from numpy import typing as npt
        import numpy as np
        import numba as nb
        
        bench_api_metrics_init()
        """;

        this.api = Bench.create(testInst);
        this.controller = new DeephavenDockerController(api.property("docker.compose.file", ""),
                api.property("deephaven.addr", ""));
        restartServices();
        api.query(query).execute();
    }

    void addServiceLog(Bench api) {
        var timer = api.timer();
        var logText = controller.getLog();
        if (logText.isBlank())
            return;
        api.log().add("deephaven-engine", logText);
        var metrics = new Metrics(Timer.now(), "test-runner", "teardown.services");
        metrics.set("log", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    void restartServices() {
        var timer = api.timer();
        if (!controller.restartService())
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.services");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }
    
    void stopUnusedServices(Set<String> keepServices) {
        var timer = api.timer();
        if (!controller.stopService(keepServices))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.services");
        metrics.set("stop", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    void generateTable(String name, String distribution, String[] groups) {
        var isNew = generateNamedTable(name, distribution, groups);
        if (isNew) {
            if (!api.isClosed()) {
                api.setName("# Data Table Generation " + name);
                addServiceLog(api);
                api.close();
            }
            initialize(testInst);
            // This should not necessary. Why does DH need it?
            generateNamedTable(name, distribution, groups);
        }
    }

    boolean generateNamedTable(String name, String distribution, String[] groups) {
        return switch (name) {
            case "source" -> generateSourceTable(distribution, groups);
            case "right" -> generateRightTable(distribution, groups);
            case "timed" -> generateTimedTable(distribution, groups);
            default -> throw new RuntimeException("Undefined table name: " + name);
        };
    }

    boolean generateSourceTable(String distribution, String[] groups) {
        return api.table("source")
                .add("num1", "double", "[0-4]", distribution)
                .add("num2", "double", "[1-10]", distribution)
                .add("key1", "string", "[1-100]", distribution)
                .add("key2", "string", "[1-101]", distribution)
                .add("key3", "int", "[0-8]", distribution)
                .add("key4", "int", "[0-98]", distribution)
                .add("key5", "string", "[1-1000000]", distribution)
                .withRowCount(getGeneratedRowCount())
                .withColumnGrouping(groups)
                .generateParquet();
    }

    boolean generateRightTable(String distribution, String[] groups) {
        if (distribution == null && api().property("default.data.distribution", "").equals("descending")) {
            distribution = "descending";
        } else {
            distribution = "ascending";
        }
        supportTables.add("right");
        return api.table("right")
                .add("r_key1", "string", "[1-100]", distribution)
                .add("r_key2", "string", "[1-101]", distribution)
                .add("r_wild", "string", "[1-10000]", distribution)
                .add("r_key4", "int", "[0-98]", distribution)
                .add("r_key5", "string", "[1-1010000]", distribution)
                .withRowCount(1010000)
                .withColumnGrouping(groups)
                .generateParquet();
    }

    boolean generateTimedTable(String distribution, String[] groups) {
        long minTime = 1676557157537L;
        long maxTime = minTime + getGeneratedRowCount() - 1;
        return api.table("timed")
                .add("timestamp", "timestamp-millis", "[" + minTime + "-" + maxTime + "]", "ascending")
                .add("num1", "double", "[0-4]", distribution)
                .add("num2", "double", "[1-10]", distribution)
                .add("key1", "string", "[1-100]", distribution)
                .add("key2", "string", "[1-101]", distribution)
                .add("key3", "int", "[0-8]", distribution)
                .add("key4", "int", "[0-98]", distribution)
                .withFixedRowCount(true)
                .withColumnGrouping(groups)
                .generateParquet();
    }

    record Result(long loadedRowCount, Duration elapsedTime, long resultRowCount) {
    }

}
