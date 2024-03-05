/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling weighted-average. The result table
 * contains additional columns with windowed rolling weighted-averages for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingWAvgTimeTest</code>
 */
public class RollingWAvgTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_wavg_tick
        contains_row = rolling_wavg_tick('num2',cols=["Contains=num1"],rev_ticks=4000,fwd_ticks=5000)
        before_row = rolling_wavg_tick('num2',cols=["Before=num1"],rev_ticks=3000,fwd_ticks=-1000)
        after_row = rolling_wavg_tick('num2',cols=["After=num1"],rev_ticks=-1000,fwd_ticks=3000)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    void rollingWAvgTick0Group3Ops() {
        runner.setRowFactor(3);
        runner.tables("timed");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingWAvgTick- 3 Ops No Groups", q, "num1", "num2");
    }

    @Test
    void rollingWAvgTick1Group3Ops() {
        runner.setScaleFactors(2, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingWAvgTick- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1", "num2");
    }

    @Test
    void rollingWAvgTime2Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingWAvgTick- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2");
    }

    @Test
    void rollingWAvgTime3Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingWAvgTick- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "num2");
    }

}
