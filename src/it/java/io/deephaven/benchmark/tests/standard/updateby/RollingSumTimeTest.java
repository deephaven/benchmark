/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling sum. The result table contains
 * additional columns with windowed rolling sums for each specified column in the source table.
 */
public class RollingSumTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @BeforeEach
    public void setup() {
        long baseTime = 1676557157537L;
        runner.api().table("source").fixed()
                .add("timestamp", "timestamp-millis", "[" + baseTime + "-" + (baseTime + rowCount - 1) + "]")
                .add("intScale", "int", "[1-" + (rowCount - 1) + "]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        var setup = """
        from deephaven.updateby import rolling_sum_time
        contains_row = rolling_sum_time(ts_col="timestamp", cols=["X=intScale"], rev_time="00:00:01", fwd_time="00:00:01")
        before_row = rolling_sum_time(ts_col="timestamp", cols=["Y=intScale"], rev_time="00:00:03", fwd_time=int(-1e9))
        after_row = rolling_sum_time(ts_col="timestamp", cols=["Z=intScale"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        """;
        runner.api().query(setup).execute();
    }

    @Test
    public void rollingSumTime0Group2Cols() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingSumTime- No Groups 2 Cols", rowCount, q, "intScale", "timestamp");
    }

    @Test
    public void rollingSumTime1Group3Cols() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingSumTime- 1 Group 100 Unique Vals 3 Cols", rowCount, q, "str100", "intScale", "timestamp");
    }

    @Test
    public void rollingSumTime2Groups4Cols() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingSumTime- 2 Groups 160K Unique Combos 4 Cols", rowCount, q, "str100", "str150",
                "intScale", "timestamp");
    }

}
