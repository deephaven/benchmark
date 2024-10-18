/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving maximum for specified
 * columns and places the result into a new column for each row.
 * <p>
 * Note: This test must contain benchmarks and <code>decay_time</code> that are comparable to <code>EmMaxTickTest</code>
 */
public class EmMaxTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    @Tag("Iterate")
    void emMaxTime0Group1Col() {
        setup.factors(5, 10, 8);
        setup.emTime0Groups("emmax_time");
        var q = "timed.update_by(ops=[dk])";
        runner.test("EmMaxTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void emMaxTime1Group1Col() {
        setup.factors(5, 5, 1);
        setup.emTime1Group("emmax_time");
        var q = "timed.update_by(ops=[dk], by=['key1'])";
        runner.test("EmMaxTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void emMaxTime2Groups1Col() {
        setup.factors(2, 3, 1);
        setup.emTime2Groups("emmax_time");
        var q = "timed.update_by(ops=[dk], by=['key1','key2'])";
        runner.test("EmMaxTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void emMaxTime3Groups1Col() {
        setup.factors(1, 3, 1);
        setup.emTime3Groups("emmax_time");
        var q = "timed.update_by(ops=[dk], by=['key1','key2','key3'])";
        runner.test("EmMaxTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "timestamp");
    }

}
