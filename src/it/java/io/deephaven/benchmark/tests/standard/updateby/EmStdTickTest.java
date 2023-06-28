/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving standard deviation for
 * specified columns and places the result into a new column for each row.
 */
public class EmStdTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emstd_tick");
    }

    @Test
    public void emStdTick0Group1Col() {
        runner.setScaleFactors(15, 8);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=int5']))";
        runner.test("EmStdTick- No Groups 1 Col", q, "int5");
    }

    @Test
    public void emStdTick0Group2Cols() {
        runner.setScaleFactors(8, 4);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=int5','Y=int10']))";
        runner.test("EmStdTick- No Groups 2 Cols", q, "int5", "int10");
    }

    @Test
    public void emStdTick1Group1Col() {
        runner.setScaleFactors(6, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=int5']), by=['str100'])";
        runner.test("EmStdTick- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5");
    }

    @Test
    public void emStdTick1Group2Cols() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("EmStdTick- 1 Group 100 Unique Vals 2 Cols", q, "str100", "int5", "int10");
    }

    @Test
    public void emStdTick2GroupsInt() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmStdTick- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150",
                "int5");
    }

    @Test
    public void emStdTick2GroupsFloat() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=100,cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmStdTick- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150",
                "float5");
    }

}
