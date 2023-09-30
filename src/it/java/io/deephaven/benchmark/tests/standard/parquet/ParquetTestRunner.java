package io.deephaven.benchmark.tests.standard.parquet;

import java.time.Duration;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Timer;

class ParquetTestRunner {
    final Object testInst;
    final Bench api;
    final long scaleRowCount;

    ParquetTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = initialize(testInst);
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
    }

    void runReadTest(String testName, String codec, String...columnNames) {
        var q = """
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        source = read('/data/source.${colName}.${codec}.parquet')
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
        q = q.replace("${colName}", formatNames(columnNames));
        q = q.replace("${codec}", codec);
        runTest(testName, q);
    }

    void runWriteTest(String testName, String codec, String...columnNames) {
        var q = """
        source = empty_table(${rowCount}).update([
            ${generators}
        ])
        
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        write(
            source, '/data/source.${colName}.${codec}.parquet', compression_codec_name='${codec}', 
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
        q = q.replace("${colName}", formatNames(columnNames));
        q = q.replace("${codec}", codec);
        q = q.replace("${generators}", getGenerators(columnNames));
        runTest(testName, q);
    }

    void runTest(String testName, String query) {
        try {
            api.setName(testName);
            api.query(query).fetchAfter("stats", table -> {
                long rowCount = table.getSum("processed_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                // long resultRowCount = table.getSum("result_row_count").longValue();
                api.result().test("deephaven-engine", Duration.ofNanos(elapsedNanos), rowCount);
            }).fetchAfter("standard_metrics", table -> {
                api.metrics().add(table);
            }).execute();
        } finally {
            api.close();
        }
    }

    String getGenerators(String...columnNames) {
        var gens = "";
        for(String columnName: columnNames) {
            gens += "'" + columnName + "=" + getGenerator(columnName) + "'\n"; 
        }
        return gens;
    }
    
    String getGenerator(String columnName) {
        String g = "";
        switch (columnName) {
            case "str10K":
                g = "(ii % 10 == 0) ? null : (`` + (ii % 10000))";
                break;
            case "long10K":
                g = "(ii % 10 == 0) ? null : (ii % 10000)";
                break;
            case "int10K":
                g = "(ii % 10 == 0) ? null : ((int)(ii % 10000))";
                break;
            case "short10K":
                g = "(ii % 10 == 0) ? null : ((short)(ii % 10000))";
                break;
            case "bigDec10K":
                g = "(ii % 10 == 0) ? null : java.math.BigDecimal.valueOf(ii % 10000)";
                break;
            default:
                throw new RuntimeException("Undefined column: " + columnName);
        }
        return g;
    }
    
    String formatNames(String...names) {
        return String.join("_", names);
    }

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import empty_table, garbage_collect
        from deephaven.column import long_col, double_col
        from deephaven.parquet import read, write
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

}
