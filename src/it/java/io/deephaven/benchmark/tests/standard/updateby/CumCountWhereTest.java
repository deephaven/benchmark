/* Copyright (c) 2025-2025 Deephaven Data Labs and Patent Pending */
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
        runner.addSetupQuery("from deephaven.updateby import cum_count_where");
    }

    @Test
    void cumCountWhere0Group1Col() {
        setup(5, 10, 10);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']))";
        runner.test("CumCountWhere- No Groups 1 Col", q, "num1");
    }

    @Test
    void cumCountWhere1Group1Col() {
        setup(3, 10, 2);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']), by=['key1'])";
        runner.test("CumCountWhere- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void cumCountWhere2Groups1Col() {
        setup(2, 3, 1);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']), by=['key1','key2'])";
        runner.test("CumCountWhere- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void cumCountWhere3Groups1Col() {
        setup(1, 3, 1);
        var q = "timed.update_by(ops=cum_count_where(col='X', filters=['num1 > 3']), by=['key1','key2','key3'])";
        runner.test("CumCountWhere- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
