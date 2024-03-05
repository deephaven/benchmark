/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the maxBy table operation. Returns the maximum value for each group.
 */
public class MaxByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void maxBy0Groups() {
        runner.setScaleFactors(35, 1);
        var q = "source.max_by()";
        runner.test("MaxBy- No Groups", 1, q, "key1", "key2", "num1");
    }

    @Test
    public void maxBy1Group() {
        runner.setScaleFactors(15, 4);
        var q = "source.max_by(by=['key1'])";
        runner.test("MaxBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    public void maxBy2Groups() {
        runner.setScaleFactors(2, 1);
        var q = "source.max_by(by=['key1', 'key2'])";
        runner.test("MaxBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    public void maxBy3Groups() {
        runner.setScaleFactors(4, 1);
        var q = "source.max_by(by=['key1', 'key2', 'key3'])";
        runner.test("MaxBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
