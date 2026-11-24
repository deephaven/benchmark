/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling count. The result table contains
 * additional columns with windowed rolling count1 for each specified column in the source table.
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingCountTimeTest</code>
 */
public class RollingCountTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @BeforeEach
    void setup() {
        runner.tables("timed");
    }

    @Test
    void rollingCountTick0Group3Ops() {
        runner.setScaleFactors(41, 35);
        setup.rollTick0Groups("rolling_count_tick");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingCountTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void rollingCountTick1Group3Ops() {
        runner.setScaleFactors(22, 4.8);
        setup.rollTick1Group("rolling_count_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingCountTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingCountTick2Groups3Ops() {
        runner.setScaleFactors(6.4, 2.3);
        setup.rollTick2Groups("rolling_count_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingCountTick- 2 Groups 10K Unique Combos", q, "key1", "key2",
                "num1");
    }

    @Test
    void rollingCountTick3Groups3Ops() {
        runner.setScaleFactors(2.9, 0.55);
        setup.rollTick3Groups("rolling_count_tick");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingCountTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
