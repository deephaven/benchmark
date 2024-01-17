/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving average for specified
 * columns and places the result into a new column for each row.
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
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']))";
        runner.test("EmsTime- No Groups 1 Col", q, "int5", "timestamp");
    }

    @Test
    public void emsTime1Group1Col() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100'])";
        runner.test("EmsTime- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5", "timestamp");
    }

    @Test
    public void emsTime2GroupsInt() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmsTime- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150", "int5", "timestamp");
    }

    @Test
    public void emsTime2GroupsFloat() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=ems_time(ts_col='timestamp', decay_time='PT2S', cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmsTime- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150", "float5", "timestamp");
    }

}
