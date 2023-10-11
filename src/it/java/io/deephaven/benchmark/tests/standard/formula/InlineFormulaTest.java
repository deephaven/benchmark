/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for running basic formulas (not functions) inline for select, update, view, update_view, etc. These
 * tests aggregate the resulting values to make sure the formulas are actually run (as in the case of views). Theses
 * tests are meant to be compared, and so use the same data.
 */
public class InlineFormulaTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final String calcs = "'New1 = (float)((int640 + int250) / 2)', 'New2 = int640 - int250'";
    
    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void selectCalcsFormula() {
        runner.setScaleFactors(120, 50);
        var q = "source.select(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calcs);
        runner.test("Select-Sum 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }
    
    @Test
    public void updateCalcsFormula() {
        runner.setScaleFactors(100, 80);
        var q = "source.update([${calcs}]).sum_by()".replace("${calcs}", calcs);
        runner.test("Update-Sum 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }
    
    @Test
    public void viewCalcsFormula() {
        runner.setScaleFactors(120, 100);
        var q = "source.view(['int250','int640',${calcs}]).sum_by()".replace("${calcs}", calcs);
        runner.test("View-Sum 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }
    
    @Test
    public void updateViewCalcsFormula() {
        runner.setScaleFactors(120, 120);
        var q = "source.update_view([${calcs}]).sum_by()".replace("${calcs}", calcs);
        runner.test("UpdateView-Sum 2 Calcs Using 2 Cols", 1, q, "int250", "int640");
    }
    
}
