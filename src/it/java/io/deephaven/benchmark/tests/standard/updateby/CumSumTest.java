/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative sum for specified columns and places the
 * result into a new column for each row.
 */
public class CumSumTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import cum_sum");
    }

    @Test
    public void cumSum0Group1Col() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=int5']))";
        runner.test("CumSum- No Groups 1 Col", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void cumSum0Group2Cols() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=int5','Y=int10']))";
        runner.test("CumSum- No Groups 2 Cols", runner.scaleRowCount, q, "int5", "int10");
    }

    @Test
    public void cumSum1Group1Col() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=int5']), by=['str100'])";
        runner.test("CumSum- 1 Group 100 Unique Vals 1 Cols", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void cumSum1Group2Cols() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("CumSum- 1 Group 100 Unique Vals 2 Cols", runner.scaleRowCount, q, "str100", "int5", "int10");
    }

    @Test
    public void cumSum2GroupsInt() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=int5']), by=['str100','str150'])";
        runner.test("CumSum- 2 Groups 15K Unique Combos 1 Col Int", runner.scaleRowCount, q, "str100", "str150", "int5");
    }
    
    @Test
    public void cumSum2GroupsFloat() {
        var q = "timed.update_by(ops=cum_sum(cols=['X=float5']), by=['str100','str150'])";
        runner.test("CumSum- 2 Groups 15K Unique Combos 1 Col Float", runner.scaleRowCount, q, "str100", "str150", "float5");
    }

}
