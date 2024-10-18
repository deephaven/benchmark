/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling weighted-average. The result table
 * contains additional columns with windowed rolling weighted-averages for each specified column in the source table. *
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingWAvgTimeTest</code>
 */
public class RollingWAvgTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);
    final String thousands = """
        from deephaven.updateby import rolling_wavg_tick
        contains_row = rolling_wavg_tick('num2',cols=["Contains=num1"],rev_ticks=2000,fwd_ticks=3000)     
        """;
    final String fifty = """ 
        from deephaven.updateby import rolling_wavg_tick
        contains_row = rolling_wavg_tick('num2',cols=["Contains=num1"],rev_ticks=20,fwd_ticks=30)
        """;

    @Test
    void rollingWAvgTick0Group3Ops() {
        setup.factors(4, 1, 1);
        runner.addSetupQuery(thousands);
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingWAvgTick- No Groups 1 Col", q, "num1", "num2");
    }

    @Test
    void rollingWAvgTick1Group3Ops() {
        setup.factors(4, 5, 2);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingWAvgTick- 1 Group 100 Unique Vals", q, "key1", "num1", "num2");
    }

    @Test
    void rollingWAvgTick2Groups3Ops() {
        setup.factors(2, 4, 1);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingWAvgTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2");
    }

    @Test
    void rollingWAvgTick3Groups3Ops() {
        setup.factors(1, 3, 1);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingWAvgTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "num2");
    }

}
