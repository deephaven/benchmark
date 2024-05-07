/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for running basic formulas (not functions) inline for select, update, view, update_view, etc. These
 * tests aggregate the resulting values to make sure the formulas are actually run (as in the case of views). These
 * tests are meant to be compared, and so use the same data.
 */
public class InlineFormulaTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final String calc1col1 = "'New1 = (float)((num2 + num2) / 2)'";
    final String calc1cols2 = "'New1 = (float)((num2 + num1) / 2)'";
    final String calc2cols2 = "'New1 = (float)((num2 + num1) / 2)', 'New2 = (float)(num2 - num1)'";

    void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);
    }

//    @Test
//    void select1Calc1ColFormula() {
//        setup(6, 22, 9);
//        var q = "source.select(['num2',${calcs}]).sum_by()".replace("${calcs}", calc1col1);
//        runner.test("Select-Sum- 1 Calc Using 1 Col", 1, q, "num2");
//    }
//
//    @Test
//    void select1Calc2ColsFormula() {
//        setup(6, 15, 8);
//        var q = "source.select(['num1','num2',${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
//        runner.test("Select-Sum- 1 Calc Using 2 Cols", 1, q, "num1", "num2");
//    }
//
//    @Test
//    void select2Calcs2ColsFormula() {
//        setup(6, 13, 8);
//        var q = "source.select(['num1','num2',${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
//        runner.test("Select-Sum- 2 Calcs Using 2 Cols", 1, q, "num1", "num2");
//    }
//
//    @Test
//    void update1Calc1ColsFormula() {
//        setup(6, 30, 18);
//        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc1col1);
//        runner.test("Update-Sum- 1 Calc Using 1 Col", 1, q, "num2");
//    }
//
//    @Test
//    void update1Calc2ColsFormula() {
//        setup(6, 22, 18);
//        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
//        runner.test("Update-Sum- 1 Calc Using 2 Cols", 1, q, "num1", "num2");
//    }

    @Test
    void update2Calcs2ColsFormula() {
        setup(6, 16, 12);
        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
        runner.test("Update-Sum- 2 Calcs Using 2 Cols", 1, q, "num1", "num2");
    }

//    @Test
//    void view1Calc1ColFormula() {
//        setup(6, 27, 25);
//        var q = "source.view(['num2',${calcs}]).sum_by()".replace("${calcs}", calc1col1);
//        runner.test("View-Sum- 1 Calc Using 1 Col", 1, q, "num2");
//    }
//
//    @Test
//    void view1Calc2ColsFormula() {
//        setup(6, 22, 22);
//        var q = "source.view(['num1','num2',${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
//        runner.test("View-Sum- 1 Calc Using 2 Cols", 1, q, "num1", "num2");
//    }
//
//    @Test
//    void view2Calcs2ColsFormula() {
//        setup(6, 17, 15);
//        var q = "source.view(['num1','num2',${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
//        runner.test("View-Sum- 2 Calcs Using 2 Cols", 1, q, "num1", "num2");
//    }
//
//    @Test
//    void updateView1Calc1ColFormula() {
//        setup(6, 35, 35);
//        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc1col1);
//        runner.test("UpdateView-Sum- 1 Calc Using 1 Col", 1, q, "num2");
//    }
//
//    @Test
//    void updateView1Calc2ColsFormula() {
//        setup(6, 22, 20);
//        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
//        runner.test("UpdateView-Sum- 1 Calc Using 2 Cols", 1, q, "num1", "num2");
//    }
//
//    @Test
//    void updateView2Calcs2ColsFormula() {
//        setup(6, 17, 17);
//        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
//        runner.test("UpdateView-Sum- 2 Calcs Using 2 Cols", 1, q, "num1", "num2");
//    }

}
