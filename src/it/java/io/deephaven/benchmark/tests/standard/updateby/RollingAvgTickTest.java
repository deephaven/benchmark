/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling average. The result table contains
 * additional columns with windowed rolling averages for each specified column in the source table. *
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingAvgTimeTest</code>
 */
public class RollingAvgTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingAvgTick0Group3Ops() {
        setup.factors(4, 5, 2);
        setup.rollTick0Groups("rolling_avg_tick");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingAvgTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void rollingAvgTick1Group3Ops() {
        setup.factors(4, 6, 1);
        setup.rollTick1Group("rolling_avg_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingAvgTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingAvgTime2Groups3Ops() {
        setup.factors(2, 4, 1);
        setup.rollTick2Groups("rolling_avg_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingAvgTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingAvgTime3Groups3Ops() {
        setup.factors(1, 3, 1);
        setup.rollTick3Groups("rolling_avg_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingAvgTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
