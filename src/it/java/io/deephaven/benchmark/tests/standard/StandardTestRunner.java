package io.deephaven.benchmark.tests.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import io.deephaven.benchmark.api.Bench;

/**
 * A wrapper for the Bench api that allows the running of small (single-operation) tests without requiring the
 * boilerplate logic like imports, parquet reads, time measurement logic, etc. Each <code>test</code> runs two tests;
 * one reading from a static parquet, and the other exercising ticking tables through the
 * <code>AutotuningIncrementalReleaseFilter</code>. Note: This class is meant to keep the majority of single-operations
 * compact and readable, not to cover every possible case. Standard query API code can be used in conjunction as long as
 * conventions are followed (ex. main file is "source")
 */
public class StandardTestRunner {
    final public long scaleRowCount;
    final Object testInst;
    final List<String> setupQueries = new ArrayList<>();
    final List<String> supportTables = new ArrayList<>();
    private String mainTable = "source";
    private Bench api;

    public StandardTestRunner(Object testInst) {
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
     * @param name
     */
    public void tables(String... names) {
        if (names.length > 0)
            mainTable = names[0];
        
        for (String name : names) {
            switch (name) {
                case "source":
                    generateSourceTable();
                    break;
                case "right":
                    generateRightTable();
                    break;
                case "timed":
                    generateTimedTable();
                    break;
                default:
                    throw new RuntimeException("Undefined table name: " + name);
            }
        }
    }

    /**
     * Add a query to be run outside the benchmark measurement but before the benchmark query
     * 
     * @param query the query to run before benchmark
     */
    public void addSetupQuery(String query) {
        setupQueries.add(query);
    }

    /**
     * Run a single-operation test through the Bench API. Create and Bench instance if necessary.
     * </p>
     * Run two tests for the operation; against static parquet, against the same parquet through ticking simulation
     * 
     * @param name the name of the test as it will show in the result file
     * @param expectedRowCount the row count expected after the operation has run
     * @param operation the operation to run and measure for the benchmark
     * @param loadColumns columns to load from the generated parquet file
     */
    public void test(String name, long expectedRowCount, String operation, String... loadColumns) {
        var staticQuery = """
        ${loadSupportTables}
        ${mainTable} = read("/data/${mainTable}.parquet").select(formulas=[${loadColumns}])

        garbage_collect()
        
        ${setupQueries}
        
        begin_time = time.perf_counter_ns()
        result = ${operation}
        end_time = time.perf_counter_ns()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0]),
            int_col("processed_row_count", [${mainTable}.size]),
            int_col("result_row_count", [result.size]),
        ])
        """;
        var rows1 = runTest(name + " -Static", expectedRowCount, staticQuery, operation, loadColumns);

        var incQuery = """ 
        ${loadSupportTables}
        loaded = read("/data/${mainTable}.parquet").select(formulas=[${loadColumns}])
        autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
        source_filter = autotune(0, 1000000, 1.0, True)
        ${mainTable} = loaded.where(source_filter)
        
        garbage_collect()
        
        ${setupQueries}

        begin_time = time.perf_counter_ns()
        result = ${operation}
        source_filter.start()
        
        UGP = jpy.get_type("io.deephaven.engine.updategraph.UpdateGraphProcessor")
        UGP.DEFAULT.requestRefresh()
        source_filter.waitForCompletion()
        end_time = time.perf_counter_ns()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0]),
            int_col("processed_row_count", [loaded.size]),
            int_col("result_row_count", [result.size]),
        ])
        """;
        var rows2 = runTest(name + " -Inc", expectedRowCount, incQuery, operation, loadColumns);
        assertTrue(rows1 == rows2, "Result row counts of static and inc tests do not match: " + rows1 + " != " + rows2);
    }

    int runTest(String name, long expectedRowCount, String query, String operation, String... loadColumns) {
        if (api.isClosed())
            api = initialize(testInst);
        api.setName(name);
        query = query.replace("${mainTable}", mainTable);
        query = query.replace("${loadSupportTables}", loadSupportTables());
        query = query.replace("${loadColumns}", listStr(loadColumns));
        query = query.replace("${setupQueries}", String.join("\n", setupQueries));
        query = query.replace("${operation}", operation);

        try {
            var elapsedMillis = new AtomicInteger();
            var rowCount = new AtomicInteger();
            api.query(query).fetchAfter("stats", table -> {
                long procRowCount = table.getSum("processed_row_count").longValue();
                long rcount = table.getSum("result_row_count").longValue();
                assertEquals(scaleRowCount, procRowCount, "Wrong processed row count");
                assertTrue(rcount > 0 && rcount <= expectedRowCount, "Wrong result row count: " + rcount);
                elapsedMillis.set(table.getSum("elapsed_millis").intValue());
                rowCount.set((int) rcount);
            }).execute();
            api.awaitCompletion();
            api.result().test(Duration.ofMillis(elapsedMillis.get()), scaleRowCount);
            return rowCount.get();
        } finally {
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

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import new_table, garbage_collect
        from deephaven.column import string_col, int_col, float_col
        from deephaven.parquet import read
        """;

        Bench api = Bench.create(testInst);
        api.query(query).execute();
        return api;
    }

    void generateSourceTable() {
        api.table("source").random()
                .add("int250", "int", "[1-250]")
                .add("int640", "int", "[1-640]")
                .add("int1M", "int", "[1-1000000]")
                .add("float5", "float", "[1-5]")
                .add("str250", "string", "s[1-250]")
                .add("str640", "string", "[1-640]s")
                .add("str1M", "string", "v[1-1000000]s")
                .generateParquet();
    }

    void generateRightTable() {
        supportTables.add("right");
        api.table("right").fixed()
                .add("r_str250", "string", "s[1-250]")
                .add("r_str640", "string", "[1-640]s")
                .add("r_int1M", "int", "[1-1000000]")
                .add("r_str1M", "string", "v[1-1000000]s")
                .add("r_str10K", "string", "r[1-100000]s")
                .generateParquet();
    }

    void generateTimedTable() {
        long baseTime = 1676557157537L;
        api.table("timed").fixed()
                .add("timestamp", "timestamp-millis", "[" + baseTime + "-" + (baseTime + scaleRowCount - 1) + "]")
                .add("int5", "int", "[1-5]")
                .add("int10", "int", "[1-10]")
                .add("float5", "float", "[1-5]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
    }

}
