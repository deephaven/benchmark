/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling maximum. The result table contains
 * additional columns with windowed rolling maximums for each specified column in the source table.
 */
@Disabled
public class RollingMaxTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @BeforeEach
    public void setup() {
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_max_time
        contains_row = rolling_max_time(ts_col="timestamp", cols=["X=int5"], rev_time="00:00:01", fwd_time="00:00:01")
        before_row = rolling_max_time(ts_col="timestamp", cols=["Y=int5"], rev_time="00:00:03", fwd_time=int(-1e9))
        after_row = rolling_max_time(ts_col="timestamp", cols=["Z=int5"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingMaxTime0Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingMaxTime- 3 Ops No Groups", rowCount, q, "int5", "timestamp");
    }

    @Test
    public void rollingMaxTime1Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingMaxTime- 3 Ops 1 Group 100 Unique Vals", rowCount, q, "str100", "int5", "timestamp");
    }

    @Test
    public void rollingMaxTime2Groups3OpsInt() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingMaxTime- 3 Ops 2 Groups 15K Unique Combos Int", rowCount, q, "str100", "str150",
                "int5", "timestamp");
    }

    @Test
    public void rollingMaxTime2Groups3OpsFloat() {
        var setup = """
        contains_row = rolling_max_time(ts_col="timestamp", cols=["X=float5"], rev_time="00:00:01", fwd_time="00:00:01")
        before_row = rolling_max_time(ts_col="timestamp", cols=["Y=float5"], rev_time="00:00:03", fwd_time=int(-1e9))
        after_row = rolling_max_time(ts_col="timestamp", cols=["Z=float5"], rev_time="-00:00:01", fwd_time=int(3e9))
        """;
        runner.addSetupQuery(setup);

        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingMaxTime- 3 Ops 2 Groups 15K Unique Combos Float", rowCount, q, "str100", "str150",
                "float5", "timestamp");
    }

}
