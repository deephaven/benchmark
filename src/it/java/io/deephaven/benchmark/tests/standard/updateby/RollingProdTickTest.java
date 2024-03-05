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

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_prod_tick
        contains_row = rolling_prod_tick(cols=["Contains=num1"], rev_ticks=4000, fwd_ticks=5000)
        before_row = rolling_prod_tick(cols=["Before=num1"], rev_ticks=3000, fwd_ticks=-1000)
        after_row = rolling_prod_tick(cols=["After=num1"], rev_ticks=-1000, fwd_ticks=3000)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    void rollingProdTick0Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingProdTick- 3 Ops No Groups", q, "num1");
    }

    @Test
    void rollingProdTick1Group3Ops() {
        runner.setScaleFactors(3, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingProdTick- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingProdTick2Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingProdTick- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingProdTick3Groups3Ops() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingProdTick- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
