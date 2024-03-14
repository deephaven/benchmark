/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the medianBy table operation
 */
public class MedianByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(2);
        runner.tables("source");
    }

    @Test
    void medianBy0Groups() {
        runner.setScaleFactors(2, 2);
        var q = "source.median_by()";
        runner.test("MedianBy- No Groups", 100, q, "key1", "key2", "num1");
    }

    @Test
    void medianBy1Group() {
        runner.setScaleFactors(12, 11);
        var q = "source.median_by(by=['key1'])";
        runner.test("MedianBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void medianBy2Groups() {
        runner.setScaleFactors(3, 3);
        var q = "source.median_by(by=['key1', 'key2'])";
        runner.test("MedianBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void medianBy3Groups() {
        var q = "source.median_by(by=['key1', 'key2', 'key3'])";
        runner.test("MedianBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
