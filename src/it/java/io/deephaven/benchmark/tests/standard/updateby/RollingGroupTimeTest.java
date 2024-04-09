/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling group. The result table contains
 * additional columns with windowed rolling groups for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingGroupTickTest</code>
 */
public class RollingGroupTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingGroupTime0Group3Ops() {
        setup.factors(5, 5, 3);
        setup.rollTime0Groups("rolling_group_time");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingGroupTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void rollingGroupTime1Group3Ops() {
        setup.factors(4, 2, 1);
        setup.rollTime1Group("rolling_group_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingGroupTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingGroupTime2Groups3Ops() {
        setup.factors(2, 2, 1);
        setup.rollTime2Groups("rolling_group_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingGroupTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingGroupTime3Groups3Ops() {
        setup.factors(1, 2, 1);
        setup.rollTime3Groups("rolling_group_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingGroupTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "timestamp");
    }

}
