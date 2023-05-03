/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the agg_by table operation. Applies aggregations to table data that require sorting
 */
public class SortedComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import agg");
    }

    @Test
    public void aggBySorted4Ops2Groups() {
        var aggs = """
        aggs = [
           agg.median('Median=int250'), agg.pct(0.50, ['Percentile=int250']), 
           agg.unique('Unique=int250'), agg.sorted_first('str250', ['int250'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Sorted-AggBy- 4 Ops 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250");
    }

    @Test
    public void aggBy3Medians2Groups() {
        var aggs = """
        aggs = [
           agg.median('Median1=int250'), agg.median('Median2=int640'), agg.median('Median3=int1M')
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Median-AggBy- 3 Medians 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }

    @Test
    public void aggBy3Percentiles2Groups() {
        var aggs = """
        aggs = [
           agg.pct(0.50, ['Percentile1=int250']), agg.pct(0.70, ['Percentile2=int640']), 
           agg.pct(0.90, ['Percentile3=int1M'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Percentile-AggBy- 3 Percentiles 2 Groups 160K Unique Vals", 160000, q, "str640", "str250",
                "int250",
                "int640", "int1M");
    }

    @Test
    public void aggBy4Sorts2Groups() {
        var aggs = """
        aggs = [
           agg.sorted_first('str250', ['int250']), agg.sorted_last('str250', ['int640']),
           agg.sorted_first('str640', ['int1M']), agg.sorted_last('str250', ['str1M'])
        ]
        """;
        runner.addSetupQuery(aggs);
        runner.setScaleFactors(10, 1);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Sorted-AggBy- 4 Sorts 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "str1M",
                "int250", "int640", "int1M");
    }

    @Test
    public void aggBy3UniqueOps2Groups() {
        var aggs = """
        aggs = [
           agg.unique('Unique1=int250'), agg.unique('Unique2=int640'), agg.unique('Unique3=int1M')
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("Unique-AggBy- 3 Unique Ops 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "int250",
                "int640", "int1M");
    }

}
