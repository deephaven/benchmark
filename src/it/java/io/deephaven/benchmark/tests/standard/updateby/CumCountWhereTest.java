/* Copyright (c) 2025-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative countWhere for specified columns and places
 * the result into a new column for each row.
 */
public class CumCountWhereTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.setScaleFactors(staticFactor, incFactor);
        runner.tables("timed");
        runner.addSetupQuery("""
        from deephaven.updateby import cum_count_where
        from deephaven.filters import or_
        """);
    }

    @Test
    void cumCountWhereRange0Group1Col() {
        setup(5, 10, 0);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']))";
        runner.test("CumCountWhere- Range No Groups 1 Col", q, "num1");
    }
    
    @Test
    void cumCountWhereEquals0Group1Col() {
        setup(5, 8, 0);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 % 3 = 0']))";
        runner.test("CumCountWhere- Equals No Groups 1 Col", q, "num1");
    }
    
    @Test
    void cumCountWhereAnd0Group1Col() {
        setup(5, 10, 0);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3','num1 % 3 = 0']))";
        runner.test("CumCountWhere- And No Groups 1 Col", q, "num1");
    }
    
    @Test
    void cumCountWhereOr0Group1Col() {
        setup(5, 3, 0);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=or_(['num1 > 3','num1 % 3 = 0'])))";
        runner.test("CumCountWhere- Or No Groups 1 Col", q, "num1");
    }

    @Test
    void cumCountWhereRange1Group1Col() {
        setup(3, 10, 0);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']), by=['key1'])";
        runner.test("CumCountWhere- Range 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void cumCountWhereEquals2Groups1Col() {
        setup(2, 3, 1);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 % 3 = 0']), by=['key1','key2'])";
        runner.test("CumCountWhere- Equals 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

}
