/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines multiple rolling operations
 */
public class RollingComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @BeforeEach
    public void setup() {
        runner.tables("timed");

        // TODO: Replace duplicate rolling sum with average when ready
        var setup = """
        from deephaven.updateby import rolling_sum_time, cum_sum, rolling_sum_time, rolling_sum_tick
        contains_row = rolling_sum_time(ts_col="timestamp", cols=["X=int5"], rev_time="00:00:01", fwd_time="00:00:01")
        before_row = rolling_sum_tick(cols=["Y=int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_sum_time(ts_col="timestamp", cols=["Z=int5"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingCombo4OpsNoGroups() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row, cum_sum(cols=['W=int5'])])";
        runner.test("RollingCombo- 4 Calcs No Groups", rowCount, q, "int5", "timestamp");
    }

    @Test
    public void rollingCombo4Ops2Groups() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row, cum_sum(cols=['W=int5'])], by=['str100','str150'])";
        runner.test("RollingCombo- 2 Groups 4 Ops Int 160K Unique Combos", rowCount, q, "str100", "str150",
                "int5", "timestamp");
    }

}
