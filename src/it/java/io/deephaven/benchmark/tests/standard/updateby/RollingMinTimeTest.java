/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling minimum. The result table contains
 * additional columns with windowed rolling minimums for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingMinTickTest</code>
 */
public class RollingMinTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingMinTime0Group3Ops() {
        setup.factors(2, 2, 2);
        setup.rollTime0Groups("rolling_min_time");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingMinTime- 3 Ops No Groups", q, "num1", "timestamp");
    }

    @Test
    void rollingMinTime1Group3Ops() {
        setup.factors(2, 2, 1);
        setup.rollTime1Group("rolling_min_time");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingMinTime- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingMinTime2Groups3Ops() {
        setup.factors(1, 2, 1);
        setup.rollTime2Groups("rolling_min_time");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingMinTime- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingMinTime3Groups3Ops() {
        setup.factors(1, 2, 1);
        setup.rollTime3Groups("rolling_min_time");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingMinTime- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "timestamp");
    }

}
