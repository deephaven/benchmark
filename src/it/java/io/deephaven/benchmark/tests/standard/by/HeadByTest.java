/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the headBy table operation. Returns the first n rows for each group.
 */
public class HeadByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void headBy1Group() {
        runner.setScaleFactors(8, 2);
        var q = "source.head_by(2, by=['key1'])";
        runner.test("HeadBy- 1 Group 100 Unique Vals", 100 * 2, q, "key1", "num1");
    }

    @Test
    public void headBy2Groups() {
        var q = "source.head_by(2, by=['key1', 'key2'])";
        runner.test("HeadBy- 2 Groups 10K Unique Combos", 10100 * 2, q, "key1", "key2", "num1");
    }

    @Test
    public void headBy3Groups() {
        var q = "source.head_by(2, by=['key1', 'key2', 'key3'])";
        runner.test("HeadBy- 3 Groups 100K Unique Combos", 90900 * 2, q, "key1", "key2", "key3", "num1");
    }

}
