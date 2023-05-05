/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.update;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the view table operation. Creates a new formula table that includes one column for each argument
 */
public class ViewTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void view1CalcUsing2Cols() {
        runner.setScaleFactors(200, 40);
        var q = "source.view(formulas=['New1 = (int640 + int250) / 2'])";
        runner.test("View- 1 Calc Using 2 Cols", q, "str250", "int250", "int640", "int1M");
    }

    @Test
    public void view2CalcsInt() {
        runner.setScaleFactors(100, 20);
        var q = "source.view(formulas=['New1 = (int640 + int250) / 2', 'New2 = int1M - int640'])";
        runner.test("View- 2 Cals Using 2 Cols", q, "str250", "int250", "int640", "int1M");
    }
    
    @Test
    public void view2CalcsFloat() {
        runner.setScaleFactors(100, 20);
        var q = "source.view(formulas=['New1 = (float5 + int250) / 2', 'New2 = int1M - float5'])";
        runner.test("View- 2 Calcs Using Float", q, "str250", "int250", "float5", "int1M");
    }

}
