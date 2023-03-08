/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the agg_by table operation. Applies weighted aggregations to table data
 */
public class WeightedComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import agg");
    }

    @Test
    public void aggBy4Calcs2Groups() {
        var aggs = """
        aggs = [
           agg.weighted_sum('int250', ['WeightedSum1=int640']), agg.weighted_avg('int250', ['WeightedAvg1=int640']),
           agg.weighted_sum('int250', ['WeightedSum2=int1M']), agg.weighted_avg('int640', ['WeightedAvg2=int1M'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Assorted-AggBy- 4 Weighted Ops 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }
    
    @Test
    public void aggBy3WeightedSums2Groups() {
        var aggs = """
        aggs = [
           agg.weighted_sum('int250', ['WeightedSum1=int640']), agg.weighted_sum('int250', ['WeightedSum2=int1M']),
           agg.weighted_sum('int640', ['WeightedSum3=int1M'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("WeightedSum-AggBy- 3 Sums 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }
    
    @Test
    public void aggBy3WeightedAvgs2Groups() {
        var aggs = """
        aggs = [
           agg.weighted_avg('int250', ['WeightedAvg1=int640']), agg.weighted_avg('int250', ['WeightedAvg2=int1M']),
           agg.weighted_avg('int640', ['WeightedAvg3=int1M'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("WeightedAvg-AggBy- 3 Sums 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }

}
