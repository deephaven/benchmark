/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving minimum for specified
 * columns and places the result into a new column for each row.
 */
public class EmMinTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emmin_time");
    }

    @Test
    public void emMinTime0Group1Col() {
        var q = "timed.update_by(ops=emmin_time(ts_col='timestamp', decay_time='00:00:02', cols=['X=int5']))";
        runner.test("EmMinTime- No Groups 1 Col", q, "int5", "timestamp");
    }

    @Test
    public void emMinTime1Group1Col() {
        var q = "timed.update_by(ops=emmin_time(ts_col='timestamp', decay_time='00:00:02', cols=['X=int5']), by=['str100'])";
        runner.test("EmMinTime- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5", "timestamp");
    }

    @Test
    public void emMinTime2GroupsInt() {
        var q = "timed.update_by(ops=emmin_time(ts_col='timestamp', decay_time='00:00:02', cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmMinTime- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150",
                "int5", "timestamp");
    }

    @Test
    public void emMinTime2GroupsFloat() {
        var q = "timed.update_by(ops=emmin_time(ts_col='timestamp', decay_time='00:00:02', cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmMinTime- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150",
                "float5", "timestamp");
    }

}
