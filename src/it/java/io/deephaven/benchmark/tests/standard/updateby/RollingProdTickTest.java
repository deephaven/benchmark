/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling product. The result table contains
 * additional columns with windowed rolling product for each specified column in the source table.
 */
public class RollingProdTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_prod_tick
        contains_row = rolling_prod_tick(cols=["Contains = int5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_prod_tick(cols=["Before = int5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_prod_tick(cols=["After = int5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingProdTick0Group1Col() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingProdTick- No Groups 1 Cols", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void rollingProdTick1Group2Cols() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingProdTick- 1 Group 100 Unique Vals 2 Cols", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void rollingProdTick2Groups3OpsInt() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingProdTick- 2 Groups 160K Unique Combos Int", runner.scaleRowCount, q, "str100", "str150",
                "int5");
    }

    @Test
    public void rollingProdTick2Groups3OpsFloat() {
        var setup = """
        contains_row = rolling_prod_tick(cols=["Contains = float5"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_prod_tick(cols=["Before = float5"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_prod_tick(cols=["After = float5"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
        
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("RollingProdTick- 2 Groups 160K Unique Combos Float", runner.scaleRowCount, q, "str100", "str150",
                "float5");
    }

}
