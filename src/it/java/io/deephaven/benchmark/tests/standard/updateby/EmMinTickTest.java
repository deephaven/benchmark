/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving minimum for specified
 * columns and places the result into a new column for each row. *
 * <p>
 * Note: This test must contain benchmarks and <code>decay_time</code> that are comparable to <code>EmMinTickTest</code>
 */
public class EmMinTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    @Tag("Iterate")
    void emMinTick0Group1Col() {
        setup.factors(6, 14, 11);
        setup.emTick0Groups("emmin_tick");
        var q = "timed.update_by(ops=[dk])";
        runner.test("EmMinTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emMinTick1Group1Col() {
        setup.factors(5, 5, 1);
        setup.emTick1Group("emmin_tick");
        var q = "timed.update_by(ops=[dk], by=['key1'])";
        runner.test("EmMinTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void emMinTick2Group1Col() {
        setup.factors(2, 3, 1);
        setup.emTick2Groups("emmin_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2'])";
        runner.test("EmMinTick- 2 Group 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emMinTick3Groups1Col() {
        setup.factors(1, 3, 1);
        setup.emTick3Groups("emmin_tick");
        var q = "timed.update_by(ops=[dk], by=['key1','key2','key3'])";
        runner.test("EmMinTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
