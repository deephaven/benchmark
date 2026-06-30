/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import org.junit.jupiter.api.*;

/**
 * Training tests for the aggBy table operations that do ordering (e.g.. median, percentile, sorted_first/last). See
 * <code>TrainTestRunner</code> for more information.
 * 
 */
public class OrderedTrainTest {
    final TrainTestRunner runner = new TrainTestRunner(this);

    void setup(double staticRowFactor, double incRowFactor) {
        runner.tables(staticRowFactor, incRowFactor, "timed");

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
    @Disabled
    void ordered0Groups() {
        setup(145, 18);
        var q = "timed.agg_by(aggs)";
        runner.test("Ordered- No Groups", 100, q, "key3", "key4", "num1", "num2");
    }

    @Test
    void ordered2Groups() {
        setup(30, 5.7);
        var q = "timed.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("Ordered- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "key3", "key4", "num1", "num2");
    }

}
