/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the aggBy table operation. Applies aggregations to table data base on in-group position
 */
public class PositionalComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");

        var aggs = """
        from deephaven import agg
        aggs = [
           agg.first(['num1']), agg.last(['num2']), 
           agg.first(['key3']), agg.last(['key4'])
        ]
        """;
        runner.addSetupQuery(aggs);
    }

    @Test
    public void posComboAggBy4Ops0Groups() {
        runner.setScaleFactors(3, 1);
        var q = "source.agg_by(aggs)";
        runner.test("PositionCombo-AggBy- 4 Ops No Groups", 100, q, "key1", "key3", "key4", "num1", "num2");
    }

    @Test
    public void posComboAggBy4Ops1Group() {
        runner.setScaleFactors(3, 1);
        var q = "source.agg_by(aggs, by=['key1'])";
        runner.test("PositionCombo-AggBy- 4 Ops 1 Group 100 Unique Vals", 100, q, "key1", "key3", "key4", "num1",
                "num2");
    }

    @Test
    public void posComboAggBy4Ops2Groups() {
        runner.setScaleFactors(3, 1);
        var q = "source.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("PositionCombo-AggBy- 4 Ops 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void posComboAggBy4Ops3Groups() {
        runner.setScaleFactors(3, 1);
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key3'])";
        runner.test("PositionCombo-AggBy- 4 Ops 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

    @Test
    public void posComboAggBy4Ops3GroupsLarge() {
        runner.setScaleFactors(3, 1);
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key4'])";
        runner.test("PositionCombo-AggBy- 4 Ops 3 Groups 1M Unique Combos", 999900, q, "key1", "key2", "key3", "key4",
                "num1", "num2");
    }

}
