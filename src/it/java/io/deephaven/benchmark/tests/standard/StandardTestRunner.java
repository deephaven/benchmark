package io.deephaven.benchmark.tests.standard;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
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
        source = read("/data/source.parquet").select(formulas=[${loadColumns}])
        
        System = jpy.get_type('java.lang.System')
        System.gc()
        
        begin_time = time.perf_counter_ns()
        result = ${operation}
        end_time = time.perf_counter_ns()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0]),
            int_col("processed_row_count", [source.size]),
            int_col("result_row_count", [result.size]),
        ])
        """;
        var rows1 = runTest(name + " -Static", expectedRowCount, staticQuery, operation, loadColumns);

        var incQuery = """ 
        loaded = read("/data/source.parquet").select(formulas=[${loadColumns}])
        autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
        source_filter = autotune(0, 1000000, 1.0, True)
        source = loaded.where(source_filter)
        
        System = jpy.get_type('java.lang.System')
        System.gc()

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
        query = query.replace("${loadColumns}",
                String.join(", ", Arrays.stream(loadColumns).map(c -> "'" + c + "'").toList()));
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

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import new_table
        from deephaven.column import string_col, int_col, float_col
        from deephaven.parquet import read
        """;

        Bench api = Bench.create(testInst);
        api.query(query).execute();
        return api;
    }

}
