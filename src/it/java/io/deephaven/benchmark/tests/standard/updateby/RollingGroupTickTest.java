/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling group. The result table contains
 * additional columns with windowed rolling groups for each specified column in the source table. *
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingGroupTimeTest</code>
 */
public class RollingGroupTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingGroupTick0Group3Ops() {
        setup.factors(5, 14, 12);
        setup.rollTick0Groups("rolling_group_tick");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingGroupTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void rollingGroupTick1Group3Ops() {
        setup.factors(5, 5, 1);
        setup.rollTick1Group("rolling_group_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingGroupTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingGroupTick2Groups3Ops() {
        setup.factors(2, 3, 1);
        setup.rollTick2Groups("rolling_group_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingGroupTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingGroupTick3Groups3Ops() {
        setup.factors(1, 3, 1);
        setup.rollTick3Groups("rolling_group_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingGroupTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
