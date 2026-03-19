/* Copyright (c) 2025-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling countWhere. The result table contains
 * an additional column with windowed rolling countWhere.
 * <p>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingCountWhereTickTest</code>
 */
public class RollingCountWhereTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);
    final String fifty1Group = """
        from deephaven.updateby import rolling_count_where_time
        contains_row = rolling_count_where_time("timestamp",'X',filters=["num1 > 3"],rev_time="PT2S",fwd_time="PT3S")
        """;
    final String fifty2Groups = """
        from deephaven.updateby import rolling_count_where_time
        contains_row = rolling_count_where_time("timestamp",'X',filters=["num1 > 3"],rev_time="PT4M",fwd_time="PT5M")
        """;

    @Test
    void rollingCountWhereTime1Group3Ops() {
        setup.factors(4, 2, 0);
        runner.addSetupQuery(fifty1Group);
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingCountWhereTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingCountWhereTime2Groups3Ops() {
        setup.factors(2, 2, 1);
        runner.addSetupQuery(fifty2Groups);
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingCountWhereTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

}
