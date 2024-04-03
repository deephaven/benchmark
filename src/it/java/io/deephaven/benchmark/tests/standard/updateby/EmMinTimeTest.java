/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving minimum for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to
 * <code>EmMinTimeTest</code>
 */
public class EmMinTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void emMinTime0Group1Col() {
        setup.factors(5, 10, 8);
        setup.emTime0Groups("emmin_time");
        var q = "timed.update_by(ops=[dk])";
        runner.test("EmMinTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void emMinTime1Group1Col() {
        setup.factors(5, 5, 1);
        setup.emTime1Group("emmin_time");
        var q = "timed.update_by(ops=[dk], by=['key1'])";
        runner.test("EmMinTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void emMinTime2Groups1Col() {
        setup.factors(2, 3, 1);
        setup.emTime2Groups("emmin_time");
        var q = "timed.update_by(ops=[dk], by=['key1','key2'])";
        runner.test("EmMinTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void emMinTime3Groups1Col() {
        setup.factors(1, 3, 1);
        setup.emTime3Groups("emmin_time");
        var q = "timed.update_by(ops=[dk], by=['key1','key2','key3'])";
        runner.test("EmMinTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "timestamp");
    }

}
