package io.deephaven.benchmark.tests.experimental.mergescale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import io.deephaven.benchmark.api.Bench;

public class MergeScaleTest {
    final Bench api = Bench.create(this);
    final long scaleRowCount = api.propertyAsIntegral("scale.row.count", "1000");
    final long baseRowCount = scaleRowCount / 8;

    @Test
    public void normalSmallTable() {
        runTest("Merged Small Table", "small", baseRowCount, scaleRowCount / 4);
    }
    
    @Test
    public void normalMediumTable() {
        runTest("Merged Medium Table", "medium", baseRowCount, scaleRowCount / 2);
    }
    
    @Test
    public void normalLargeTable() {
        runTest("Merged Large Table", "large", baseRowCount, scaleRowCount);
    }

    @AfterEach
    public void teardown() {
        api.close();
    }
    
    private void runTest(String testName, String tableName, long baseRowCount, long rowCount) {
        api.setName(testName);
        generateTable(tableName, baseRowCount);
        long tableFactor = rowCount / baseRowCount;
        
        var query = """
        import time
        from deephaven import new_table, merge, garbage_collect
        from deephaven.column import int_col, float_col
        from deephaven.parquet import read

        ${table} = merge([read("/data/${table}.parquet")] * ${tableFactor})
        garbage_collect()
        
        begin_time = time.perf_counter_ns()
        result = ${table}.sort(['str250', 'str640'])
        end_time = time.perf_counter_ns()
        
        stats = new_table([
            float_col("elapsed_ns", [end_time - begin_time]),
            int_col("result_row_count", [${table}.size])
        ])
        """;
        query = query.replace("${table}", tableName);
        query = query.replace("${tableFactor}", "" + tableFactor);

        api.query(query).fetchAfter("stats", table -> {
            assertEquals(rowCount, table.getSum("result_row_count").longValue(), "Wrong record count");
            var duration = Duration.ofNanos(table.getSum("elapsed_ns").longValue());
            api.result().test("deephaven-engine", duration, rowCount);
        }).execute();
    }

    private void generateTable(String tableName, long rowCount) {
        api.table(tableName).random()
                .add("int250", "int", "[1-250]")
                .add("int640", "int", "[1-640]")
                .add("int1M", "int", "[1-1000000]")
                .add("float5", "float", "[1-5]")
                .add("str250", "string", "s[1-250]")
                .add("str640", "string", "[1-640]s")
                .add("str1M", "string", "v[1-1000000]s")
                .withRowCount((int)rowCount)
                .generateParquet();
    }

}
