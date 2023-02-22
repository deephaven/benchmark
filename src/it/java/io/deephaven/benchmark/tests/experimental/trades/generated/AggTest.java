/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic aggregation tests for customers
 */
public class AggTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int500", "int", "[1-500]")
                .add("int1K", "int", "[1-1000]")
                .add("str10K", "string", "s[1-10000]v")
                .generateParquet();
    }

    @Test
    public void sumBy1Group1IntCol() {
        var q = "source.sum_by(by=['str10K'])";
        runner.test("SumBy- 1 Group 10K Unique Vals 1 Int Col", 10000, q, "str10K", "int500");
    }

    @Test
    public void sumBy1Group2IntCols() {
        var q = "source.sum_by(by=['str10K'])";
        runner.test("SumBy- 1 Group 10K Unique Vals 2 Int Col", 10000, q, "str10K", "int500");
    }

    @Test
    public void aggBy1Group1IntCol() {
        var q = """
        from deephaven import agg
        aggs = [
            agg.avg('Avg1=int500'), agg.sum_('Sum1=int500'), 
            agg.count_('int500'), agg.std('Std1=int500')
        ]
        """;
        runner.addSupportQuery(q);
        
        q = "source.agg_by(aggs, by=['str10K'])";
        runner.test("AggBy-Combo- 1 Group 10K Unique Vals 1 Int Col", 10000, q, "str10K", "int500");
    }

    @Test
    public void aggBy1Group2IntCols() {
        var q = """
        from deephaven import agg
        aggs = [
            agg.avg(['Avg1=int500','Avg2=int1K']), agg.sum_(['Sum1=int500','Sum=int1K']), 
            agg.count_('int500'), agg.std(['Std1=int500','Std2=int1K'])
        ]
        """;
        runner.addSupportQuery(q);
        
        q = "source.agg_by(aggs, by=['str10K'])";
        runner.test("AggBy-Combo- 1 Group 10K Unique Vals 2 Int Col", 10000, q, "str10K", "int500", "int1K");
    }

}
