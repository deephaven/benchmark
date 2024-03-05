/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the aggBy table operation. Applies aggregations to table data that require sorting
 */
public class SortedComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");

        var setupStr = """
        from deephaven import agg
        aggs = [
           agg.median('Median=num1'), agg.pct(0.50, ['Percentile=num1']), 
           agg.unique('Unique=num2'), agg.sorted_first('key4', ['num2']),
           agg.sorted_last('key3', ['num1'])
        ]
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    public void aggBySorted5Ops0Groups() {
        var q = "source.agg_by(aggs)";
        runner.test("SortedCombo-AggBy- 5 Ops No Groups", 100, q, "key3", "key4", "num1", "num2");
    }

    @Test
    public void aggBySorted5Ops1Group() {
        var q = "source.agg_by(aggs, by=['key1'])";
        runner.test("SortedCombo-AggBy- 5 Ops 1 Group 100 Unique Vals", 100, q, "key1", "key3", "key4", "num1", "num2");
    }

    @Test
    public void aggBySorted5Ops2Groups() {
        var q = "source.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("SortedCombo-AggBy- 5 Ops 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void aggBySorted5Ops3Groups() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key3'])";
        runner.test("SortedCombo-AggBy- 5 Ops 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void aggBySorted5Ops3GroupsLarge() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key4'])";
        runner.test("SortedCombo-AggBy- 5 Ops 3 Groups 1M Unique Combos", 999900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

}
