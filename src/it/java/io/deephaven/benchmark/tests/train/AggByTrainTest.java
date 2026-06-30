/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import org.junit.jupiter.api.*;

/**
 * Training tests for the aggBy table operations that do aggregations (e.g. sum, std, min/max. var, avg). See
 * <code>TrainTestRunner</code> for more information.
 */
public class AggByTrainTest {
    final TrainTestRunner runner = new TrainTestRunner(this);

    void setup(double staticRowFactor, double incRowFactor) {
        runner.tables(staticRowFactor, incRowFactor, "timed");

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
    @Disabled
    void aggBy0Groups() {
        setup(572, 286);
        var q = "timed.agg_by(aggs)";
        runner.test("AggBy- No Groups", 1, q, "num1", "num2");
    }

    @Test
    void aggBy2Groups() {
        setup(66, 38);
        var q = "timed.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("AggBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1", "num2");
    }

}
