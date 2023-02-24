/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that match the <code>provided</code> tests.
 */
public class WhereTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.table("quotes_g", runner.getScaleRowCount());
        runner.sourceTable("quotes_g");
    }

    @Test
    public void where3Clauses() {
        var q = "quotes_g.where(filters=['(Ask - Bid) > 1', 'BidSize <= 100', 'AskSize <= 100'])";
        runner.test("Where- 3 Clauses", 2000, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

    @Test
    public void whereOneOfComboClauses() {
        var q = """
        quotes_g.where_one_of(['(Ask - Bid) > 1', 'BidSize <= 100', 'AskSize <= 100']
        ).where(["Sym in 'S1', 'S2', 'S3', 'S4', 'S5'"])
        """;
        runner.test("WhereOneOf- Where Combo", 500000, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

}
