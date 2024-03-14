/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving standard deviation for
 * specified columns and places the result into a new column for each row. *
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_time</code> that are comparable to <code>EmStdTickTest</code>
 */
public class EmStdTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(3);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emstd_time");
    }

    @Test
    void emStdTime0Group1Col() {
        runner.setScaleFactors(20, 15);
        var q = "timed.update_by(ops=emstd_time(ts_col='timestamp',decay_time='PT5S', cols=['X=num1']))";
        runner.test("EmStdTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void emStdTime1Group1Col() {
        runner.setScaleFactors(9, 2);
        var q = "timed.update_by(ops=emstd_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']),by=['key1'])";
        runner.test("EmStdTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void emStdTime2Groups1Col() {
        runner.setScaleFactors(2, 1);
        var q = """
        timed.update_by(ops=emstd_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']),by=['key1','key2'])
        """;
        runner.test("EmStdTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void emStdTime3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = """
        timed.update_by(ops=emstd_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']),by=['key1','key2','key3'])
        """;
        runner.test("EmStdTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "timestamp");
    }

}
