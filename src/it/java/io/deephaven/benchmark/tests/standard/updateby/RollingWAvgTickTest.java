/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling weighted-average. The result table
 * contains additional columns with windowed rolling weighted-averages for each specified column in the source table.
 */
public class RollingWAvgTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_wavg_tick
        contains_row = rolling_wavg_tick('int10', cols=["Contains = int5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_wavg_tick('int10', cols=["Before = int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_wavg_tick('int10', cols=["After = int5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingWAvgTick0Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingWAvgTick- 3 Ops No Groups", q, "int5", "int10");
    }

    @Test
    public void rollingWAvgTick1Group3Ops() {
        runner.setScaleFactors(2, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingWAvgTick- 3 Ops 1 Group 100 Unique Vals", q, "str100", "int5", "int10");
    }

    @Test
    public void rollingWAvgTime2Groups3OpsInt() {
        runner.setScaleFactors(2, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingWAvgTick- 3 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150",
                "int5", "int10");
    }

    @Test
    public void rollingWAvgTick2Groups3OpsFloat() {
        runner.setScaleFactors(2, 1);
        var setup = """
        contains_row = rolling_wavg_tick(weight_col='int10', cols=["Contains = float5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_wavg_tick(weight_col='int10', cols=["Before = float5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_wavg_tick(weight_col='int10', cols=["After = float5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);

        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingWAvgTick- 3 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150",
                "float5", "int10");
    }

}
