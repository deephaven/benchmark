/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
    final String calc1col1 = "'New1 = (float)((int640 + int640) / 2)'";
    final String calc1cols2 = "'New1 = (float)((int640 + int250) / 2)'";
    final String calc2cols2 = "'New1 = (float)((int640 + int250) / 2)', 'New2 = int640 - int250'";

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void select1Calc1ColFormula() {
        runner.setScaleFactors(160, 60);
        var q = "source.select(['int640',${calcs}]).sum_by()".replace("${calcs}", calc1col1);
        runner.test("Select-Sum- 1 Calc Using 1 Col", 1, q, "int640");
    }

    @Test
    public void select1Calc2ColsFormula() {
        runner.setScaleFactors(120, 50);
        var q = "source.select(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
        runner.test("Select-Sum- 1 Calc Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void select2Calcs2ColsFormula() {
        runner.setScaleFactors(120, 50);
        var q = "source.select(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
        runner.test("Select-Sum- 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void update1Calc1ColsFormula() {
        runner.setScaleFactors(160, 130);
        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc1col1);
        runner.test("Update-Sum- 1 Calc Using 1 Col", 1, q, "int640");
    }

    @Test
    public void update1Calc2ColsFormula() {
        runner.setScaleFactors(140, 100);
        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
        runner.test("Update-Sum- 1 Calc Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void update2Calcs2ColsFormula() {
        runner.setScaleFactors(110, 80);
        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
        runner.test("Update-Sum- 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void view1Calc1ColFormula() {
        runner.setScaleFactors(130, 120);
        var q = "source.view(['int640',${calcs}]).sum_by()".replace("${calcs}", calc1col1);
        runner.test("View-Sum- 1 Calc Using 1 Col", 1, q, "int640");
    }

    @Test
    public void view1Calc2ColsFormula() {
        runner.setScaleFactors(120, 120);
        var q = "source.view(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
        runner.test("View-Sum- 1 Calc Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void view2Calcs2ColsFormula() {
        runner.setScaleFactors(120, 100);
        var q = "source.view(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
        runner.test("View-Sum- 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void updateView1Calc1ColFormula() {
        runner.setScaleFactors(140, 140);
        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc1col1);
        runner.test("UpdateView-Sum- 1 Calc Using 1 Col", 1, q, "int640");
    }

    @Test
    public void updateView1Calc2ColsFormula() {
        runner.setScaleFactors(120, 120);
        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc1cols2);
        runner.test("UpdateView-Sum- 1 Calc Using 2 Cols", 1, q, "int250", "int640");
    }

    @Test
    public void updateView2Calcs2ColsFormula() {
        runner.setScaleFactors(120, 120);
        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calc2cols2);
        runner.test("UpdateView-Sum- 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }

}
