/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving maximum for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: When there are no Group Keys, EmMaxTime has a much faster rate than EmMinTime. This is likely because of branch
 * prediction on x86 systems. This disparity does not happen on Mac M1.
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
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']))";
        runner.test("EmMaxTime- No Groups 1 Col", q, "int5", "timestamp");
    }

    @Test
    void emMaxTime1Group1Col() {
        setup(6, 3, 1);
        runner.setScaleFactors(3, 1);
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100'])";
        runner.test("EmMaxTime- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5", "timestamp");
    }

    @Test
    void emMaxTime2GroupsInt() {
        setup(3, 1, 1);
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmMaxTime- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150", "int5", "timestamp");
    }

    @Test
    void emMaxTime2GroupsFloat() {
        setup(3, 1, 1);
        var q = "timed.update_by(ops=emmax_time(ts_col='timestamp', decay_time='PT2S', cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmMaxTime- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150", "float5", "timestamp");
    }

}
