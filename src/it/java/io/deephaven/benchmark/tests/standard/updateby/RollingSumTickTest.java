/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling sum. The result table contains
 * additional columns with windowed rolling sums for each specified column in the source table.
 */
public class RollingSumTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_sum_tick
        contains_row = rolling_sum_tick(cols=["Contains = int5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_sum_tick(cols=["Before = int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_sum_tick(cols=["After = int5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingSumTick0Group3Ops() {
        runner.setScaleFactors(4, 4);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingSumTick- 3 Ops No Groups", q, "int5");
    }

    @Test
    public void rollingSumTick1Group3Ops() {
        runner.setScaleFactors(4, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingSumTick- 3 Ops 1 Group 100 Unique Vals", q, "str100", "int5");
    }

    @Test
    public void rollingSumTick2Groups3OpsInt() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingSumTick- 3 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150",
                "int5");
    }

    @Test
    public void rollingSumTick2Groups3OpsFloat() {
        runner.setScaleFactors(1, 1);
        var setup = """
        contains_row = rolling_sum_tick(cols=["Contains = float5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_sum_tick(cols=["Before = float5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_sum_tick(cols=["After = float5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);

        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingSumTick- 3 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150",
                "float5");
    }

}
