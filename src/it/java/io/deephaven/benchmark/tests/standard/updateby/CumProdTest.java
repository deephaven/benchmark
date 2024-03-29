/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative product for specified columns and places the
 * result into a new column for each row.
 */
public class CumProdTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import cum_prod");
    }

    @Test
    public void cumProd0Group1Col() {
        runner.setScaleFactors(30, 15);
        var q = "timed.update_by(ops=cum_prod(cols=['X1=int5']))";
        runner.test("CumProd- No Groups 1 Col", q, "int5");
    }

    @Test
    public void cumProd0Group2Cols() {
        runner.setScaleFactors(15, 8);
        var q = "timed.update_by(ops=cum_prod(cols=['X=int5','Y=int10']))";
        runner.test("CumProd- No Groups 2 Cols",  q, "int5", "int10");
    }

    @Test
    public void cumProd1Group1Cols() {
        runner.setScaleFactors(9, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X1=int5']), by=['str100'])";
        runner.test("CumProd- 1 Group 100 Unique Vals 1 Col", q, "str100", "int5");
    }

    @Test
    public void cumProd1Group2Cols() {
        runner.setScaleFactors(4, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("CumProd- 1 Group 100 Unique Vals 2 Cols", q, "str100", "int5", "int10");
    }

    @Test
    public void cumProd2GroupsInt() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X=int5']), by=['str100','str150'])";
        runner.test("CumProd- 2 Groups 15K Unique Combos 1 Col Int", q, "str100", "str150", "int5");
    }
    
    @Test
    public void cumProd2GroupsFloat() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X=float5']), by=['str100','str150'])";
        runner.test("CumProd- 2 Groups 15K Unique Combos 1 Col Float", q, "str100", "str150", "float5");
    }

}
