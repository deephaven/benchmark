/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that could be shown to customers
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
        var q = "quotes.where(filters=['(Ask - Bid) > 1', 'BidSize = 100', 'AskSize = 100'])";
        runner.test("Where- 3 Clauses", 329093, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }
    
    @Test
    public void whereOneOfComboClauses() {
        var q = """
        quotes.where_one_of(
            filters=["Sym='META'", "Sym='AMZN'", "Sym='AAPL'", "Sym='NFLX'", "Sym='GOOG'"]
        ).where(['(Ask - Bid) > 1', 'BidSize = 100', 'AskSize = 100'])
        """;
        runner.test("WhereOneOf- Where Combo", 611, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

}
