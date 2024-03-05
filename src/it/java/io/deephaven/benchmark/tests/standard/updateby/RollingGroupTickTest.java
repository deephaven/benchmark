/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling group. The result table contains
 * additional columns with windowed rolling groups for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingGroupTimeTest</code>
 */
public class RollingGroupTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_group_tick
        contains_row = rolling_group_tick(cols=["Contains=num1"], rev_ticks=4000, fwd_ticks=5000)
        before_row = rolling_group_tick(cols=["Before=num1"], rev_ticks=3000, fwd_ticks=-1000)
        after_row = rolling_group_tick(cols=["After=num1"], rev_ticks=-1000, fwd_ticks=3000)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    void rollingGroupTick0Group3Ops() {
        runner.setScaleFactors(10, 6);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingGroupTick- 3 Ops No Groups", q, "num1");
    }

    @Test
    void rollingGroupTick1Group3Ops() {
        runner.setScaleFactors(3, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingGroupTick- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingGroupTick2Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingGroupTick- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingGroupTick3Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingGroupTick- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
