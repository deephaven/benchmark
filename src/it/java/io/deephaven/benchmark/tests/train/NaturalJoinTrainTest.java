/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import org.junit.jupiter.api.*;

/**
 * Training tests for the aggBy table operations that do joins (e.g. natural join). See <code>TrainTestRunner</code> for
 * more information.
 */
public class NaturalJoinTrainTest {
    final TrainTestRunner runner = new TrainTestRunner(this);

    void setup(double staticRowFactor, double incRowFactor) {
        runner.tables(staticRowFactor, incRowFactor, "timed", "right");
    }

    @Test
    @Disabled
    void naturalJoinOn1Col() {
        runner.setIncReleaseRowCount(448939);
        setup(230, 120);
        var r = "right = right.select_distinct(['r_wild'])";
        runner.addSetupQuery(r);
        var q = "timed.natural_join(right, on=['key1 = r_wild'])";
        runner.test("NaturalJoin- Join On 1 Col", 0, q, "key1", "num1");
    }

    @Test
    void naturalJoinOn3Cols() {
        runner.setIncReleaseRowCount(52043);
        setup(100, 20);
        var q = "timed.natural_join(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 = r_key1'])";
        runner.test("NaturalJoin- Join On 3 Cols", 0, q, "key1", "key2", "num1");
    }

}
