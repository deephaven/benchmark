/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.update;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the update_view table operation. Creates a new table containing a new formula column for each
 * argument
 */
public class UpdateViewTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void updateView1CalcUsing2Cols() {
        var q = "source.update_view(formulas=['New1 = (int640 + int250) / 2'])";
        runner.test("UpdateView- 1 Calc Using 2 Cols", runner.scaleRowCount, q, "str250", "int250", "int640", "int1M");
    }

    @Test
    public void updateView2CalcsUsing2Cols() {
        var q = "source.update_view(formulas=['New1 = (int640 + int250) / 2', 'New2 = int1M - int640'])";
        runner.test("UpdateView- 2 Cals Using 2 Cols", runner.scaleRowCount, q, "str250", "int250", "int640", "int1M");
    }

}
