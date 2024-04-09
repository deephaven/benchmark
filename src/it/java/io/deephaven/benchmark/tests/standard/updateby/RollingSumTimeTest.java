/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling sum. The result table contains
 * additional columns with windowed rolling sums for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingSumTickTest</code>
 */
public class RollingSumTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingSumTime0Group3Ops() {
        setup.factors(4, 2, 2);
        setup.rollTime0Groups("rolling_sum_time");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingSumTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void rollingSumTime1Group3Ops() {
        setup.factors(3, 3, 1);
        setup.rollTime1Group("rolling_sum_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingSumTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingSumTime2Groups3Ops() {
        setup.factors(2, 2, 1);
        setup.rollTime2Groups("rolling_sum_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingSumTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingSumTime3Groups3Ops() {
        setup.factors(1, 3, 1);
        setup.rollTime3Groups("rolling_sum_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingSumTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "timestamp");
    }

}
