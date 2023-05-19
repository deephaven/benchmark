/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.trades.TradesTestRunner;

/**
 * Basic where tests that match the <code>provided</code> tests.
 */
// TODO: autotune both sides of the join
public class JoinTest {
    final TradesTestRunner runner = new TradesTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.table("trades_g", runner.getScaleRowCount());
        runner.table("quotes_g", runner.getScaleRowCount() * 4);
        runner.sourceTable("trades_g");
        runner.addSupportTable("quotes_g");
    }

    @Test
    public void asOfJoinOn2Cols() {
        var q = "trades_g.aj(quotes_g, ['Sym', 'Timestamp'])";
        runner.test("AsOfJoin- Join On 2 Columns", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Price");
    }

    @Test
    public void asOfJoinCombo() {
        var q = """
        (
            trades_g.aj(quotes_g, ["Sym", "Timestamp"])
            .update_view(["Mid=(Bid+Ask)/2", "Edge=abs(Price-Mid)", "DollarEdge=Edge*Size"])
            .view(["Sym", "DollarEdge"])
            .sum_by(["Sym"])
        )
        """;
        runner.test("AsOfJoin- Join On 2 Columns Combo", 430, q, "Sym", "Timestamp", "Price", "Size");
    }

}
