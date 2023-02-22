/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.provided;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic aggregation tests for customers
 */
public class AggTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.sourceTable("quotes");
        runner.setScaleRowCount(84255431);
    }

    @Test
    public void sumBy1Group1IntCol() {
        var q = "quotes.sum_by(by=['Sym'])";
        runner.test("SumBy- 1 Group 1 Int Col", 431, q, "Sym", "Bid");
    }

    @Test
    public void sumBy1Group2IntCols() {
        var q = "quotes.sum_by(by=['Sym'])";
        runner.test("SumBy- 1 Group 2 Int Cols", 431, q, "Sym", "Bid", "Ask");
    }
    
    @Test
    public void sumBy2Groups2IntCols() {
        var q = "quotes.sum_by(by=['Date','Sym'])";
        runner.test("SumBy- 2 Groups 2 Int Cols", 431, q, "Sym", "Date", "Bid", "Ask");
    }

    @Test
    public void aggBy1Group1IntCol() {
        var q = """
        from deephaven import agg
        aggs = [
           agg.sum_('TotalBid=Bid'), agg.std('StdBid=Bid'),
           agg.avg('AvgBid=Bid'), agg.count_('Bid')
        ]
        """;
        runner.addSupportQuery(q);
        
        q = "quotes.agg_by(aggs, by=['Sym'])";
        runner.test("AggBy-Combo- 1 Group 4 Calcs On 1 Int Col", 431, q, "Sym", "Bid");
    }

    @Test
    public void aggBy2Group2IntCols() {
        var q = """
        from deephaven import agg
        aggs = [
            agg.min_('MinBid=Bid'), agg.max_('MaxAsk=Ask'),
            agg.avg('AvgAsk=Ask'), agg.count_('Bid')
        ]
        """;
        runner.addSupportQuery(q);
        
        q = "quotes.agg_by(aggs, by=['Date', 'Sym'])";
        runner.test("AggBy-Combo- 2 Groups 4 Calcs On 2 Int Cols", 431, q, "Sym", "Date", "Bid", "Ask");
    }

}
