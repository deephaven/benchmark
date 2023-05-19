/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.trades.TradesTestRunner;

/**
 * Basic where tests for customers. Assumes that Deephaven is already running and that there is a
 * <code>quotes.parquet</code> file and <code>trades.parquet</code> file or corresponding links in the Engine's /data
 * directory
 */
// TODO: autotune both sides of the join
public class JoinTest {
    final TradesTestRunner runner = new TradesTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.sourceTable("trades");
        runner.addSupportTable("quotes");
        runner.setScaleRowCount(21469392);
    }

    @Test
    public void asOfJoinOn3Cols() {
        var q = "trades.aj(quotes, ['Sym', 'Timestamp'])";
        runner.test("AsOfJoin- Join On 2 Columns", 21469392, q, "Sym", "Timestamp", "Price");
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
        runner.test("AsOfJoin- Join On 2 Columns Combo", runner.getScaleRowCount(), q, "Sym", "Timestamp", "Price",
                "Size");
    }

}
