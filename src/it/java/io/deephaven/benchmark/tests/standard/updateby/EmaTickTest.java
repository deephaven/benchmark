/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to <code>EmaTimeTest</code>
 */
public class EmaTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void emaTick0Group1Col() {
        setup.factors(6, 18, 14);
        setup.emTick0Groups("ema_tick");
        var q = "timed.update_by(ops=[dk])";
        runner.test("EmaTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emaTick1Group1Col() {
        setup.factors(5, 5, 1);
        setup.emTick1Group("ema_tick");
        var q = "timed.update_by(ops=[dk], by=['key1'])";
        runner.test("EmaTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void emaTick2Group1Col() {
        setup.factors(2, 3, 1);
        setup.emTick2Groups("ema_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2'])";
        runner.test("EmaTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emaTick3Groups1Col() {
        setup.factors(1, 3, 1);
        setup.emTick3Groups("ema_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2','key3'])";
        runner.test("EmaTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
