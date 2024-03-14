/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. Groups column content into arrays.
 */
public class GroupByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("source");
    }

    @Test
    void groupBy1Group() {
        runner.setScaleFactors(10, 2);
        var q = "source.group_by(by=['key1'])";
        runner.test("GroupBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void groupBy2Groups() {
        runner.setScaleFactors(2, 2);
        var q = "source.group_by(by=['key1', 'key2'])";
        runner.test("GroupBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void groupBy3Groups() {
        var q = "source.group_by(by=['key1', 'key2', 'key3'])";
        runner.test("GroupBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
