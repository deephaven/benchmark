package io.deephaven.benchmark.tests.experimental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.io.IOException;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import io.deephaven.benchmark.api.Bench;

public class ExperimentalTestRunner {
    final Object testInst;
    private long scaleRowCount;
    private Bench api;
    private String sourceTable = "source";
    private Map<String, String[]> supportTables = new LinkedHashMap<>();
    private List<String> supportQueries = new ArrayList<>();

    public ExperimentalTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = initialize(testInst);
        this.scaleRowCount = api.propertyAsIntegral("scale.row.count", "100000");
    }

    public Bench api() {
        return api;
    }

    public long getScaleRowCount() {
        return scaleRowCount;
    }

    public void setScaleRowCount(long scaleRowCount) {
        this.scaleRowCount = scaleRowCount;
    }

    public void sourceTable(String name) {
        this.sourceTable = name;
    }

    public void addSupportTable(String name, String... columns) {
        supportTables.put(name, columns);
    }
    
    public void addSupportQuery(String query) {
        supportQueries.add(query);
    }

    public void test(String name, long expectedRowCount, String operation, String... sourceTableColumns) {
        var staticQuery = """
        ${loadSupportTables}
        ${sourceTable} = read("/data/${sourceTable}.parquet").select(formulas=[${sourceColumns}])
        
        System = jpy.get_type('java.lang.System')
        System.gc()
        
        ${supportQueries}
        
        begin_time = time.perf_counter_ns()
        result = ${operation}
        end_time = time.perf_counter_ns()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0]),
            int_col("processed_row_count", [${sourceTable}.size]),
            int_col("result_row_count", [result.size]),
        ])
        """;
        var rows1 = runTest(name + " -Static", expectedRowCount, staticQuery, operation, sourceTableColumns);

        var incQuery = """ 
        ${loadSupportTables}
        loaded = read("/data/${sourceTable}.parquet").select(formulas=[${sourceColumns}])
        autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
        source_filter = autotune(0, 1000000, 1.0, False)   # initial release, release size, target factor, verbose 
        ${sourceTable} = loaded.where(source_filter)
        
        #deephaven.garbage collect
        System = jpy.get_type('java.lang.System')
        System.gc()
        
        ${supportQueries}

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
        var rows2 = runTest(name + " -Inc", expectedRowCount, incQuery, operation, sourceTableColumns);
        assertTrue(rows1 == rows2, "Result row counts of static and inc tests do not match: " + rows1 + " != " + rows2);
    }

    int runTest(String name, long expectedRowCount, String query, String operation, String... sourceTableColumns) {
        if (api.isClosed())
            api = initialize(testInst);
        api.setName(name);
        query = query.replace("${loadSupportTables}", getSupportTablesLogic());
        query = query.replace("${sourceColumns}", listStr(sourceTableColumns));
        query = query.replace("${sourceTable}", sourceTable);
        query = query.replace("${supportQueries}", String.join("\n", supportQueries));
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

    String getSupportTablesLogic() {
        String query = "";
        for (Map.Entry<String, String[]> e : supportTables.entrySet()) {
            String columns = (e.getValue().length == 0) ? "" : ("formulas=[" + listStr(e.getValue()) + "]");
            var q = "${table}= read('/data/${table}.parquet').select(${columns})";
            q = q.replace("${table}", e.getKey()).replace("${columns}", columns);
            query += q + "\n";
        }
        return query;
    }

    Bench initialize(Object testInst) {
        var query = """
        import time
        from deephaven import new_table
        from deephaven.column import string_col, int_col, float_col
        from deephaven.parquet import read
        """;

        Bench api = Bench.create(testInst);
        restartDocker(api);
        api.query(query).execute();
        return api;
    }

    void generateSourceTable() {
        api.table("source").random()
                .add("int250", "int", "[1-250]")
                .add("int640", "int", "[1-640]")
                .add("int1M", "int", "[1-1000000]")
                .add("str250", "string", "string[1-250]val")
                .add("str640", "string", "val[1-640]string")
                .add("str1M", "string", "val[1-1000000]string")
                .generateParquet();
    }

    String listStr(String... values) {
        return String.join(", ", Arrays.stream(values).map(c -> "'" + c + "'").toList());
    }

    void restartDocker(Bench api) {
        var dockerComposeFile = api.property("docker.compose.file", "");
        if (dockerComposeFile.isBlank())
            return;
        exec("docker compose -f " + dockerComposeFile + " down");
        sleep(1);
        exec("docker compose -f " + dockerComposeFile + " up -d");
        sleep(5);
    }

    void exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            if (!process.waitFor(20, TimeUnit.SECONDS))
                throw new RuntimeException("Timeout while running command: " + command);
            if (process.exitValue() != 0)
                throw new RuntimeException("Bad exit code " + process.exitValue() + " for command: " + command);
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Failed to execute command: " + command);
        }
    }

    void sleep(int secs) {
        try {
            Thread.sleep(secs * 1000);
        } catch (InterruptedException e) {
        }
    }

}
