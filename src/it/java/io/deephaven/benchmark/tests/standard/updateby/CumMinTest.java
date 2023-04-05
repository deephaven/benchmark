/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative minimum for specified columns and places the
 * result into a new column for each row.
 */
public class CumMinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import cum_min");
    }

    @Test
    public void cumMin0Group1Col() {
        var q = "timed.update_by(ops=cum_min(cols=['X=int5']))";
        runner.test("CumMin- No Groups 1 Cols", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void cumMin0Group2Cols() {
        var q = "timed.update_by(ops=cum_min(cols=['X=int5','Y=int10']))";
        runner.test("CumMin- No Groups 2 Cols", runner.scaleRowCount, q, "int5", "int10");
    }

    @Test
    public void cumMin1Group1Cols() {
        var q = "timed.update_by(ops=cum_min(cols=['X=int5']), by=['str100'])";
        runner.test("CumMin- 1 Group 100 Unique Vals 1 Col", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void cumMin1Group2Cols() {
        var q = "timed.update_by(ops=cum_min(cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("CumMin- 1 Group 100 Unique Vals 2 Cols", runner.scaleRowCount, q, "str100", "int5", "int10");
    }

    @Test
    public void cumMin2GroupsInt() {
        var q = "timed.update_by(ops=cum_min(cols=['X=int5']), by=['str100','str150'])";
        runner.test("CumMin- 2 Groups 15K Unique Combos 1 Col Int", runner.scaleRowCount, q, "str100", "str150",
                "int5");
    }

    @Test
    public void cumMin2GroupsFloat() {
        var q = "timed.update_by(ops=cum_min(cols=['X=float5']), by=['str100','str150'])";
        runner.test("CumMin- 2 Groups 15K Unique Combos 1 Col Float", runner.scaleRowCount, q, "str100", "str150", "float5");
    }

}
