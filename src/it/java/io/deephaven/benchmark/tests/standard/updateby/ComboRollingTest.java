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
        long baseTime = 1676557157537L;
        runner.api().table("source").fixed()
                .add("timestamp", "timestamp-millis", "[" + baseTime + "-" + (baseTime + rowCount - 1) + "]")
                .add("intScale", "int", "[1-" + (rowCount - 1) + "]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        // TODO: Replace duplicate rolling sum with average when ready
        var setup = """
        from deephaven.updateby import rolling_sum_time, cum_sum, rolling_sum_time, rolling_sum_tick
        contains_row = rolling_sum_time(ts_col="timestamp", cols=["X=intScale"], rev_time="00:00:01", fwd_time="00:00:01")
        before_row = rolling_sum_tick(cols=["Before = intScale"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_sum_time(ts_col="timestamp", cols=["Z=intScale"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingCombo4CalcsNoGroups() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row, cum_sum(cols=['W=intScale'])])";
        runner.test("RollingCombo- 4 Calcs No Groups", rowCount, q, "intScale", "timestamp");
    }

    @Test
    public void rollingCombo4Calcs2Groups() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row, cum_sum(cols=['W=intScale'])], by=['str100','str150'])";
        runner.test("RollingCombo- 2 Groups 160K Unique Combos 4 Cols", rowCount, q, "str100", "str150",
                "intScale", "timestamp");
    }

}
