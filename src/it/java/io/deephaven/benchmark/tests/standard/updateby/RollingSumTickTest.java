/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
        runner.api().table("source").fixed()
                .add("intScale", "int", "[1-" + runner.scaleRowCount + "]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        var setup = """
        from deephaven.updateby import rolling_sum_tick
        contains_row = rolling_sum_tick(cols=["Contains = intScale"], rev_ticks=1, fwd_ticks=1)
        before_row = rolling_sum_tick(cols=["Before = intScale"], rev_ticks=3, fwd_ticks=-1)
        after_row = rolling_sum_tick(cols=["After = intScale"], rev_ticks=-1, fwd_ticks=3)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void rollingSumTick0Group1Col() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingSumTick- No Groups 1 Cols", runner.scaleRowCount, q, "intScale");
    }

    @Test
    public void rollingSumTick1Group2Cols() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row], by=['str100'])";
        runner.test("RollingSumTick- 1 Group 100 Unique Vals 2 Cols", runner.scaleRowCount, q, "str100", "intScale");
    }

    @Test
    public void rollingSumTick2Groups3Cols() {
        var q = "source.update_by(ops=[contains_row, before_row, after_row], by=['str100','str150'])";
        runner.test("CumSum- 2 Groups 160K Unique Combos 3 Cols", runner.scaleRowCount, q, "str100", "str150",
                "intScale");
    }

}
