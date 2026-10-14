/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the descending sort table operation. Sorts rows of data from the source table according to the
 * defined columns.
 */
public class SortDescendingTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int staticFactor, int incFactor) {
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);
    }

    @Test
    void sort1Col() {
        setup(3, 1);
        var q = "source.sort(order_by=['key1'])";
        runner.test("Sort- 1 Col Descending", q, "key1", "num1");
    }

    @Test
    void sort2Cols() {
        setup(3, 1);
        var q = "source.sort(order_by=['key1', 'key2'])";
        runner.test("Sort- 2 Cols Descending", q, "key1", "key2", "num1");
    }

    @Test
    void sort3Cols() {
        setup(3, 1);
        var q = "source.sort(order_by=['key1', 'key2', 'key3'])";
        runner.test("Sort- 3 Cols Descending", q, "key1", "key2", "key3", "num1");
    }

}
