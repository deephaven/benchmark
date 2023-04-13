/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic update tests that match the <code>provided</code> tests.
 */
public class UpdateTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.table("quotes_g", runner.getScaleRowCount());
        runner.sourceTable("quotes_g");
    }
    
    @Test
    public void update1CalcUsing2Cols() {
        var q = "quotes_g.update(formulas=['Mid=(Bid+Ask)/2'])";
        runner.test("Update- 1 Calc Using 2 Cols", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Bid", "Ask");
    }
    
    @Test
    public void update2CalcsUsing2Cols() {
        var q = "quotes_g.update(formulas=['Mid=(Bid+Ask)/2', 'Spread=Ask-Bid'])";
        runner.test("Update- 2 Calcs Using 2 Cols", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Bid", "Ask");
    }

}
