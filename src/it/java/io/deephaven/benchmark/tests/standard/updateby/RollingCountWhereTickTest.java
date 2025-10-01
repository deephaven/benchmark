/* Copyright (c) 2025-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling countWhere. The result table contains
 * an additional column with windowed rolling countWhere.
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingCountWhereTimeTest</code>
 */
public class RollingCountWhereTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);
    final String thousands = """
        from deephaven.updateby import rolling_count_where_tick
        contains_row = rolling_count_where_tick('X',filters=["num1 > 3"],rev_ticks=2000,fwd_ticks=3000)     
        """;
    final String fifty = """ 
        from deephaven.updateby import rolling_count_where_tick
        contains_row = rolling_count_where_tick('X',filters=["num1 > 3"],rev_ticks=20,fwd_ticks=30)
        """;

    @Test
    void rollingCountWhereTick0Group3Ops() {
        setup.factors(5, 8, 8);
        runner.addSetupQuery(thousands);
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingCountWhereTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void rollingCountWhereTick1Group3Ops() {
        setup.factors(5, 4, 1);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingCountWhereTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingCountWhereTick2Groups3Ops() {
        setup.factors(2, 3, 1);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingCountWhereTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingCountWhereTick3Groups3Ops() {
        setup.factors(1, 3, 1);
        runner.addSetupQuery(fifty);
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingCountWhereTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
