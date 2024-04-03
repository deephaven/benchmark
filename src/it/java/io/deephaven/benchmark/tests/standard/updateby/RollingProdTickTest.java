/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling product. The result table contains
 * additional columns with windowed rolling product for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingProdTimeTest</code>
 */
public class RollingProdTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @Test
    void rollingProdTick0Group3Ops() {
        setup.factors(6, 2, 2);
        setup.rollTick0Groups("rolling_prod_tick");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingProdTick- 3 Ops No Groups", q, "num1");
    }

    @Test
    void rollingProdTick1Group3Ops() {
        setup.factors(4, 3, 1);
        setup.rollTick1Group("rolling_prod_tick");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingProdTick- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingProdTick2Groups3Ops() {
        setup.factors(2, 2, 1);
        setup.rollTick2Groups("rolling_prod_tick");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingProdTick- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingProdTick3Groups3Ops() {
        setup.factors(1, 2, 1);
        setup.rollTick3Groups("rolling_prod_tick");
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingProdTick- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
