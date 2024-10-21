/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving minimum for specified
 * columns and places the result into a new column for each row.
 * <p>
 * Note: When there are no Group Keys, EmMinTick has a much slower rate than EmMaxTick. This is likely because of branch
 * prediction on x86 systems. This disparity does not happen on Mac M1.
 */
public class EmMinTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emmin_tick");
    }

    @Test
    public void emMinTick0Group1Col() {
        runner.setScaleFactors(4, 3);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=int5']))";
        runner.test("EmMinTick- No Groups 1 Col", q, "int5");
    }

    @Test
    public void emMinTick0Group2Cols() {
        runner.setScaleFactors(2, 2);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=int5','Y=int10']))";
        runner.test("EmMinTick- No Groups 2 Cols", q, "int5", "int10");
    }

    @Test
    public void emMinTick1Group1Col() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=int5']), by=['str100'])";
        runner.test("EmMinTick- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5");
    }

    @Test
    public void emMinTick1Group2Cols() {
        runner.setScaleFactors(4, 1);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("EmMinTick- 1 Group 100 Unique Vals 2 Cols", q, "str100", "int5", "int10");
    }

    @Test
    public void emMinTick2GroupsInt() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmMinTick- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150",
                "int5");
    }

    @Test
    public void emMinTick2GroupsFloat() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=emmin_tick(decay_ticks=100,cols=['X=float5']), by=['str100','str150'])";
        runner.test("EmMinTick- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150",
                "float5");
    }

}
