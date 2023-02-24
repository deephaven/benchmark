/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic update tests for customers. Assumes that Deephaven is already running and that there is a
 * <code>quotes.parquet</code> file or link in the Engine's /data directory
 */
public class UpdateTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.sourceTable("quotes");
        runner.setScaleRowCount(84255431);
    }

    @Test
    public void update1CalcUsing2Cols() {
        var q = "quotes.update(formulas=['Mid=(Bid+Ask)/2'])";
        runner.test("Update- 1 Calc Using 2 Cols", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Bid", "Ask");
    }

    @Test
    public void update2CalcsUsing2Cols() {
        var q = "quotes.update(formulas=['Mid=(Bid+Ask)/2', 'Spread=Ask-Bid'])";
        runner.test("Update- 2 Cals Using 2 Cols", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Bid", "Ask");
    }

}
