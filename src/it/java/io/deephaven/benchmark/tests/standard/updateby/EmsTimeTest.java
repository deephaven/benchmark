/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving average for specified
 * columns and places the result into a new column for each row. *
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_time</code> that are comparable to <code>EmsTickTest</code>
 */
public class EmsTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import ems_time");
    }

    @Test
    public void emsTime0Group1Col() {
        runner.setScaleFactors(11, 9);
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp', decay_time='PT5S', cols=['X=num1']))";
        runner.test("EmsTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    public void emsTime1Group1Col() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']),by=['key1'])";
        runner.test("EmsTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    public void emsTime2Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = """
        timed.update_by(ops=ems_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']), by=['key1','key2'])
        """;
        runner.test("EmsTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    public void emsTime3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = """
        timed.update_by(ops=ems_time(ts_col='timestamp',decay_time='PT5S',cols=['X=num1']), 
            by=['key1','key2','key3'])
        """;
        runner.test("EmsTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "timestamp");
    }

}
