/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that could be shown to customers
 */

// TODO: autotune both sides of the join
public class JoinTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.sourceTable("trades");
        runner.addSupportTable("quotes");
        runner.setScaleRowCount(21469392);
    }

    @Test
    public void asOfJoinOn3Cols() {
        var q = "trades.aj(quotes, ['Sym', 'Timestamp'])";
        runner.test("AsOfJoin- Join On 3 Columns", 21469392, q, "Sym", "Timestamp", "Price");
    }

    @Test
    public void asOfJoinCombo() {
        var q = """
        (
            trades.aj(quotes, ["Sym", "Timestamp"])
            .update_view(["Mid=(Bid+Ask)/2", "Edge=abs(Price-Mid)", "DollarEdge=Edge*Size"])
            .view(["Sym", "DollarEdge"])
            .sum_by(["Sym"])
        )
        """;
        runner.test("AsOfJoin- Join On 3 Columns Combo", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Price", "Size");
    }

}
