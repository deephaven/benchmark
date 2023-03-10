/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests for customers. Assumes that Deephaven is already running and that there is a
 * <code>quotes.parquet</code> file or link in the Engine's /data directory
 */
public class WhereTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.sourceTable("quotes");
        runner.setScaleRowCount(84255431);
    }

    @Test
    public void where3Clauses() {
        var q = "quotes.where(filters=['(Ask - Bid) > 1', 'BidSize <= 100', 'AskSize <= 100'])";
        runner.test("Where- 3 Clauses", 376084, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

    @Test
    public void whereOneOfComboClauses() {
        var q = """
        quotes.where_one_of(['(Ask - Bid) > 1', 'BidSize <= 100', 'AskSize <= 100']
        ).where(["Sym in 'META', 'AMZN', 'AAPL', 'NFLX', 'GOOG'"])
        """;
        runner.test("WhereOneOf- Where Combo", 1949322, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

}
