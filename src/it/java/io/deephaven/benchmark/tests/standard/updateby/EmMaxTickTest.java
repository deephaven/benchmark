/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving maximum for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to
 * <code>EmMaxTimeTest</code>
 */
public class EmMaxTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void emMaxTick0Group1Col() {
        setup.factors(5, 14, 12);
        setup.emTick0Groups("emmax_tick");
        var q = "timed.update_by(ops=[dk])";
        runner.test("EmMaxTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emMaxTick1Group1Col() {
        setup.factors(5, 5, 1);
        setup.emTick1Group("emmax_tick");
        var q = "timed.update_by(ops=[dk], by=['key1'])";
        runner.test("EmMaxTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void emMaxTick2Groups1Col() {
        setup.factors(2, 3, 1);
        setup.emTick2Groups("emmax_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2'])";
        runner.test("EmMaxTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emMaxTick3Groups1Col() {
        setup.factors(1, 3, 1);
        setup.emTick3Groups("emmax_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2','key3'])";
        runner.test("EmMaxTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
