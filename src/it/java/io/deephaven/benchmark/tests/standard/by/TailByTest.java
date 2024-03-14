/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the tailBy table operation. Returns the last n rows for each group.
 */
public class TailByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("source");
    }

    @Test
    void tailBy1Group() {
        runner.setScaleFactors(10, 2);
        var q = "source.tail_by(2, by=['key1'])";
        runner.test("TailBy- 1 Group 100 Unique Vals", 100 * 2, q, "key1", "num1");
    }

    @Test
    void tailBy2Groups() {
        runner.setScaleFactors(2, 2);
        var q = "source.tail_by(2, by=['key1', 'key2'])";
        runner.test("TailBy- 2 Groups 10K Unique Combos", 10100 * 2, q, "key1", "key2", "num1");
    }

    @Test
    void tailBy3Groups() {
        var q = "source.tail_by(2, by=['key1', 'key2', 'key3'])";
        runner.test("TailBy- 3 Groups 100K Unique Combos", 90900 * 2, q, "key1", "key2", "key3", "num1");
    }

}
