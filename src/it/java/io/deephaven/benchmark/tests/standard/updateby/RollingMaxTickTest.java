/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling maximum. The result table contains
 * additional columns with windowed rolling maximum for each specified column in the source table.
 */
@Disabled
public class RollingMaxTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_max_tick
        contains_row = rolling_max_tick(cols=["Contains = int5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_max_tick(cols=["Before = int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_max_tick(cols=["After = int5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingMaxTick0Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingMaxTick- 3 Ops No Groups", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void rollingMaxTick1Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingMaxTick- 3 Ops 1 Group 100 Unique Vals", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void rollingMaxTick2Groups3OpsInt() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingMaxTick- 3 Ops 2 Groups 15K Unique Combos Int", runner.scaleRowCount, q, "str100", "str150",
                "int5");
    }

    @Test
    public void rollingMaxTick2Groups3OpsFloat() {
        var setup = """
        contains_row = rolling_max_tick(cols=["Contains = float5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_max_tick(cols=["Before = float5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_max_tick(cols=["After = float5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
        
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingMaxTick- 3 Ops 2 Groups 15K Unique Combos Float", runner.scaleRowCount, q, "str100", "str150",
                "float5");
    }

}
