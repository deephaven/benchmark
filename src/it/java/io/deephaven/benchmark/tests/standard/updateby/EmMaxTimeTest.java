/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving maximum for specified
 * columns and places the result into a new column for each row. *
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_time</code> that are comparable to <code>EmMaxTickTest</code>
 */
public class EmMaxTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emmax_time");
        runner.setScaleFactors(staticFactor, incFactor);
    }

    @Test
    void emMaxTime0Group1Col() {
        setup(6, 11, 8);
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']))";
        runner.test("EmMaxTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void emMaxTime1Group1Col() {
        setup(6, 3, 1);
        runner.setScaleFactors(3, 1);
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']), by=['key1'])";
        runner.test("EmMaxTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void emMaxTime2Groups1Col() {
        setup(3, 1, 1);
        var q = """
        timed.update_by(ops=emmax_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']), by=['key1','key2'])
        """;
        runner.test("EmMaxTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void emMaxTime2GroupsFloat() {
        setup(3, 1, 1);
        var q = """
        timed.update_by(ops=emmax_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']), 
            by=['key1','key2','key3'])
        """;
        runner.test("EmMaxTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "timestamp");
    }

}
