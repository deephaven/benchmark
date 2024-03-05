/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
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
        runner.setRowFactor(5);
        runner.tables("source");

        var setupStr = """
        from deephaven import agg
        aggs = [
           agg.weighted_sum('key4', ['WeightedSum1=num1']), agg.weighted_avg('key3', ['WeightedAvg1=num2']),
           agg.weighted_sum('key4', ['WeightedSum2=num1']), agg.weighted_avg('key3', ['WeightedAvg2=num2'])
        ]
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    public void weightedAggBy4Ops0Groups() {
        var q = "source.agg_by(aggs)";
        runner.test("WeightedCombo-AggBy- 4 Ops No Groups", 1, q, "key3", "key4", "num1", "num2");
    }

    @Test
    public void weightedAggBy4Ops1Group() {
        var q = "source.agg_by(aggs, by=['key1'])";
        runner.test("WeightedCombo-AggBy- 4 Ops 1 Group 100 Unique Vals", 100, q, "key1", "key3", "key4", "num1",
                "num2");
    }

    @Test
    public void weightedAggBy4Ops2Groups() {
        var q = "source.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("WeightedCombo-AggBy- 4 Ops 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void weightedAggBy4Ops3Groups() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key3'])";
        runner.test("WeightedCombo-AggBy- 4 Ops 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void weightedAggBy4Ops3GroupsLarge() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key4'])";
        runner.test("WeightedCombo-AggBy- 4 Ops 3 Groups 1M Unique Combos", 999900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

}
