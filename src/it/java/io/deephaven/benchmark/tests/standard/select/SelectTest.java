/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.select;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the select table operation. Creates a new in-memory table that includes one column for each
 * argument
 */
public class SelectTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void select1CalcUsing2Cols() {
        runner.setScaleFactors(200, 40);
        var q = "source.select(formulas=['New1 = (float)((int640 + int250) / 2)'])";
        runner.test("Select- 1 Calc Using 2 Cols", q, "int250", "int640");
    }

    @Test
    public void select2CalcsUsing2Cols() {
        runner.setScaleFactors(100, 20);
        var q = "source.select(formulas=['New1 = (float)((int640 + int250) / 2)', 'New2 = int640 - int250'])";
        runner.test("Select- 2 Cals Using 2 Cols", q, "int250", "int640");
    }

}
