/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the sumBy table operation. Returns the total sum for each group.
 */
public class SumByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void sumBy0Group() {
        runner.setScaleFactors(10, 10);
        var q = "source.sum_by()";
        runner.test("SumBy- No Groups", 1, q, "key3", "num1", "num2");
    }

    @Test
    public void sumBy1Group() {
        runner.setScaleFactors(10, 10);
        var q = "source.sum_by(by=['key1'])";
        runner.test("SumBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    public void sumBy2Groups() {
        runner.setScaleFactors(3, 2);
        var q = "source.sum_by(by=['key1', 'key2'])";
        runner.test("SumBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    public void sumBy3Groups() {
        runner.setScaleFactors(3, 2);
        var q = "source.sum_by(by=['key1', 'key2', 'key3'])";
        runner.test("SumBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
