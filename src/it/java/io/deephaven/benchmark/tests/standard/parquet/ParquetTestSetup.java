package io.deephaven.benchmark.tests.standard.parquet;

import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Timer;

class ParquetTestSetup {
    final public long scaleRowCount;
    final Object testInst;
    final Bench api;

    ParquetTestSetup(Object testInst) {
        this.testInst = testInst;
        this.api = initialize(testInst);
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
    }

    void table(String name, String codec) {
        if (!name.equals("compressed"))
            throw new RuntimeException("Undefined table name: " + name);
        generateCompressedTable(codec);
    }

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import new_table, garbage_collect
        from deephaven.column import int_col, float_col
        from deephaven.parquet import read, write
        """;

        Bench api = Bench.create(testInst);
        restartDocker(api);
        api.query(query).execute();
        return api;
    }

    void restartDocker(Bench api) {
        var timer = api.timer();
        if (!Exec.restartDocker(api.property("docker.compose.file", "")))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup", "docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    void generateCompressedTable(String codec) {
        api.table("compressed").random()
                .add("str100K", "string", "SYM[1-100000]")
                .add("str10K", "string", "SYM[1-10000]")
                .add("long100K", "long", "[1-100000]")
                .add("long10K", "long", "[1-10000]")
                .add("double100K", "double", "[1-100000]")
                .add("double10K", "double", "[1-10000]")
                .withCompression(codec)
                .generateParquet();
    }

}
