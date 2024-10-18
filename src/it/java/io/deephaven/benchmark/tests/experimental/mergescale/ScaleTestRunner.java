package io.deephaven.benchmark.tests.experimental.mergescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.controller.DeephavenDockerController;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Timer;

/**
 * A wrapper for the Bench api that ensures that the scale tests run the same way and with comparable generated data.
 * This tests a sort operation based on generated rows of data. The expected row count is achieved by fully generating
 * the data to a parquet file or partially generating the data and merging that by <code>tableFactor</code> to get row
 * count.
 * <p>
 * Note: For best results, use base and row counts that are highly divisible and clear like 1,000,000 so that the
 * <code>tableFactor</code> is a whole number.
 */
class ScaleTestRunner {
    final Bench api;
    final long scaleRowCount;

    ScaleTestRunner(Object testInst) {
        api = Bench.create(this);
        scaleRowCount = api.propertyAsIntegral("scale.row.count", "1000");
        restartDocker(api);
    }

    void runTest(String testName, String tableName, long baseRowCount, long rowCount) {
        api.setName(testName);
        generateTable(tableName, baseRowCount);
        long tableFactor = rowCount / baseRowCount;

        var query = """
        import time
        from deephaven import new_table, merge, garbage_collect, agg
        from deephaven.column import int_col, float_col
        from deephaven.parquet import read
        
        bench_api_metrics_init()

        ${table} = ${readTable}
        garbage_collect()
        
        aggs = [
           agg.pct(0.50, ['Percentile1=int250']), agg.pct(0.70, ['Percentile2=int640']), 
           agg.pct(0.90, ['Percentile3=int1M'])
        ]
        
        begin_time = time.perf_counter_ns()
        result = ${table}.agg_by(aggs, by=['str250', 'str640'])
        end_time = time.perf_counter_ns()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            float_col("elapsed_ns", [end_time - begin_time]),
            int_col("result_row_count", [${table}.size])
        ])
        """;
        var readTable = "read('/data/${table}.parquet').select()";
        if (tableFactor > 1)
            readTable =
                    "merge([${readTable}] * ${tableFactor}).update_view(['row_id=ii', 'int250=int250+1', 'int640=int640+1', 'int1M=int1M+1'])"
                            .replace("${readTable}", readTable);

        query = query.replace("${readTable}", readTable);
        query = query.replace("${table}", tableName);
        query = query.replace("${tableFactor}", "" + tableFactor);

        try {
            api.query(query).fetchAfter("stats", table -> {
                assertEquals(rowCount, table.getSum("result_row_count").longValue(), "Wrong record count");
                var duration = Duration.ofNanos(table.getSum("elapsed_ns").longValue());
                api.result().test("deephaven-engine", duration, rowCount);
            }).fetchAfter("standard_metrics", table -> {
                api.metrics().add(table);
            }).execute();
        } finally {
            api.close();
        }
    }

    void restartDocker(Bench api) {
        var timer = api.timer();
        var controller = new DeephavenDockerController(api.property("docker.compose.file", ""), api.property("deephaven.addr", ""));
        if (!controller.restartService())
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    void generateTable(String tableName, long rowCount) {
        api.table(tableName)
                .add("int250", "int", "[1-250]")
                .add("int640", "int", "[1-640]")
                .add("int1M", "int", "[1-1000000]")
                .add("float5", "float", "[1-5]")
                .add("str250", "string", "s[1-250]")
                .add("str640", "string", "[1-640]s")
                .add("str1M", "string", "v[1-1000000]s")
                .withRowCount((int) rowCount)
                .generateParquet();
    }

}
