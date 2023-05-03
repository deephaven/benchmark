/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling group. The result table contains
 * additional columns with windowed rolling groups for each specified column in the source table.
 */
public class RollingGroupTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_group_tick
        contains_row = rolling_group_tick(cols=["Contains = int5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_group_tick(cols=["Before = int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_group_tick(cols=["After = int5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingGroupTick0Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingGroupTick- 3 Ops No Groups", q, "int5");
    }

    @Test
    public void rollingGroupTick1Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingGroupTick- 3 Ops 1 Group 100 Unique Vals", q, "str100", "int5");
    }

    @Test
    public void rollingGroupTick2Groups3OpsInt() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingGroupTick- 3 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150",
                "int5");
    }

    @Test
    public void rollingGroupTick2Groups3OpsFloat() {
        var setup = """
        contains_row = rolling_group_tick(cols=["Contains = float5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_group_tick(cols=["Before = float5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_group_tick(cols=["After = float5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
        
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingGroupTick- 3 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150",
                "float5");
    }

}
