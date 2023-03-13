/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 */
public class EmaTimeDecayTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @BeforeEach
    public void setup() {
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import ema_time_decay");
    }

    @Test
    public void emaTimeDecay0Group2Cols() {
        var q = "timed.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=int5']))";
        runner.test("EmaTimeDecay- No Groups 1 Col", rowCount, q, "int5", "timestamp");
    }

    @Test
    public void emaTimeDecay1Group3Cols() {
        var q = "timed.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=int5']), by=['str100'])";
        runner.test("EmaTimeDecay- 1 Group 100 Unique Vals 2 Col", rowCount, q, "str100", "int5", "timestamp");
    }

    @Test
    public void emaTimeDecay2GroupsInt() {
        var q = "timed.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmaTickDecay- 2 Groups 160K Unique Combos Int", runner.scaleRowCount, q, "str100", "str150",
                "int5", "timestamp");
    }
    
    @Test
    public void emaTimeDecay2GroupsFloat() {
        var q = "timed.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmaTickDecay- 2 Groups 160K Unique Combos Float", runner.scaleRowCount, q, "str100", "str150",
                "float5", "timestamp");
    }

}
