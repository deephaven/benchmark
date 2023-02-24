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
        runner.tables("quotes");
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
        quotes.where_one_of(['(Ask - Bid) > 1', 'BidSize = 100', 'AskSize = 100']
        ).where(["Sym in '1', 'S2', 'S3', 'S4', 'S5'"])
        """;
        runner.test("WhereOneOf- Where Combo", 19419322, q, "Sym", "Timestamp", "Bid", "BidSize", "Ask", "AskSize");
    }

}
