/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the aggBy table operation. Applies basic math aggregations to table data
 */
public class BasicMathComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(3);
        runner.tables("source");

        var setupStr = """
        from deephaven import agg
        
        aggs = [
           agg.sum_('Sum=num1'), agg.std('Std=num2'), agg.min_('Min=num1'), agg.max_('Max=num2'),
           agg.avg('Avg=num1'), agg.var('Var=num2'), agg.count_('num1')
        ]
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void mathComboAggBy7Ops0Groups() {
        runner.setScaleFactors(20, 9);
        var q = "source.agg_by(aggs)";
        runner.test("MathCombo-AggBy- 7 Ops No Groups", 1, q, "num1", "num2");
    }

    @Test
    void mathComboAggBy7Ops1Group() {
        runner.setScaleFactors(9, 4);
        var q = "source.agg_by(aggs, by=['key1'])";
        runner.test("MathCombo-AggBy- 7 Ops 1 Group 100 Unique Vals ", 100, q, "key1", "num1", "num2");
    }

    @Test
    void mathComboAggBy7Ops2Groups() {
        runner.setScaleFactors(2, 1);
        var q = "source.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("MathCombo-AggBy- 7 Ops 2 Groups 10K Unique Combos ", 10100, q, "key1", "key2", "num1", "num2");
    }

    @Test
    void mathComboAggBy7Ops3Groups() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key3'])";
        runner.test("MathCombo-AggBy- 7 Ops 3 Groups 100K Unique Combos ", 90900, q, "key1", "key2", "key3", "num1",
                "num2");
    }

    @Test
    void mathComboAggBy7Ops3GroupsLarge() {
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key4'])";
        runner.test("MathCombo-AggBy- 7 Ops 3 Groups 1M Unique Combos ", 999900, q, "key1", "key2", "key4", "num1",
                "num2");
    }

}
