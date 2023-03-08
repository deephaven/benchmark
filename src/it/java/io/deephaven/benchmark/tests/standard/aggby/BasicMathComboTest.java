/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the agg_by table operation. Applies a list of basic math aggregations to table data
 */
public class BasicMathComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import agg");
    }

    @Test
    public void basicMathAggBy7Calcs2Groups() {
        var aggs = """
        aggs = [
           agg.sum_('Sum=int250'), agg.std('Std=int250'), agg.min_('Min=int250'), agg.max_('Max=int250'),
           agg.avg('Avg=int250'), agg.var('Var=int250'), agg.count_('int250')
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("BasicMath-AggBy- 7 Calcs 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250");
    }

    @Test
    public void basicMathAggBy3Sums2Groups() {
        var aggs = """
        aggs = [
           agg.sum_('Sum1=int250'), agg.sum_('Sum2=int640'), agg.sum_('Sum3=int1M')
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("BasicMath-AggBy- 3 Sums 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }

    @Test
    public void basicMathAggBy3Vars2Groups() {
        var aggs = """
        aggs = [
           agg.sum_('Var1=int250'), agg.sum_('Var2=int640'), agg.sum_('Var3=int1M')
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("BasicMath-AggBy- 3 Vars 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }

}
