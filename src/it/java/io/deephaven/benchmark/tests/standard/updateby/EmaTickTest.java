/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 */
public class EmaTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import ema_tick");
    }

    @Test
    public void emaTick0Group1Col() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=int5']))";
        runner.test("EmaTick- No Groups 1 Col", q, "int5");
    }

    @Test
    public void emaTick0Group2Cols() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=int5','Y=int10']))";
        runner.test("EmaTick- No Groups 2 Cols", q, "int5", "int10");
    }

    @Test
    public void emaTick1Group1Col() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=int5']), by=['str100'])";
        runner.test("EmaTick- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5");
    }

    @Test
    public void emaTick1Group2Cols() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("EmaTick- 1 Group 100 Unique Vals 2 Cols", q, "str100", "int5", "int10");
    }

    @Test
    public void emaTick2GroupsInt() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmaTick- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150",
                "int5");
    }

    @Test
    public void emaTick2GroupsFloat() {
        var q = "timed.update_by(ops=ema_tick(time_scale_ticks=100,cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmaTick- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150",
                "float5");
    }

}
