/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the avgBy table operation. Returns the average (mean) of each non-key column for each group.
 */
public class AvgByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }
    
    @Test
    void avgBy0Group() {
        runner.setScaleFactors(40, 40);
        var q = "source.avg_by()";
        runner.test("AvgBy- No Groups", 1, q, "key3", "num1", "num2");
    }

    @Test
    void avgBy1Group() {
        runner.setScaleFactors(11, 10);
        var q = "source.avg_by(by=['key1'])";
        runner.test("AvgBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void avgBy2Groups() {
        runner.setScaleFactors(3, 2);
        var q = "source.avg_by(by=['key1', 'key2'])";
        runner.test("AvgBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void avgBy3Groups() {
        runner.setScaleFactors(2, 1);
        var q = "source.avg_by(by=['key1', 'key2', 'key3'])";
        runner.test("AvgBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
