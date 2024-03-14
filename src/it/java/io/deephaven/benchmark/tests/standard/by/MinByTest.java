/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the minBy table operation. Returns the minimum value for each group.
 */
public class MinByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("source");
    }

    @Test
    void minBy0Groups() {
        runner.setScaleFactors(20, 1);
        var q = "source.min_by()";
        runner.test("MinBy- No Groups", 1, q, "key1", "key2", "num1");
    }

    @Test
    void minBy1Group() {
        runner.setScaleFactors(18, 8);
        var q = "source.min_by(by=['key1'])";
        runner.test("MinBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void minBy2Groups() {
        runner.setScaleFactors(4, 2);
        var q = "source.min_by(by=['key1', 'key2'])";
        runner.test("MinBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void minBy3Groups() {
        runner.setScaleFactors(3, 1);
        var q = "source.min_by(by=['key1', 'key2', 'key3'])";
        runner.test("MinBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
