/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a time-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 */
public class EmaTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import ema_time");
    }

    @Test
    public void emaTime0Group1Col() {
        var q = "timed.update_by(ops=ema_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']))";
        runner.test("EmaTime- No Groups 1 Col", q, "int5", "timestamp");
    }

    @Test
    public void emaTime1Group1Col() {
        var q = "timed.update_by(ops=ema_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100'])";
        runner.test("EmaTime- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5", "timestamp");
    }

    @Test
    public void emaTime2GroupsInt() {
        var q = "timed.update_by(ops=ema_time(ts_col='timestamp', decay_time='PT2S', cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmaTime- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150",
                "int5", "timestamp");
    }
    
    @Test
    public void emaTime2GroupsFloat() {
        var q = "timed.update_by(ops=ema_time(ts_col='timestamp', decay_time='PT2S', cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmaTime- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150",
                "float5", "timestamp");
    }

}
