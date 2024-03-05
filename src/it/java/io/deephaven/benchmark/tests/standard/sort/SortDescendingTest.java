/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the descending sort table operation. Sorts rows of data from the source table according to the
 * defined columns.
 */
public class SortDescendingTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void sort1Col() {
        var q = "source.sort(order_by=['key1'])";
        runner.test("Sort- 1 Col Descending", q, "key1", "num1");
    }

    @Test
    public void sort2Cols() {
        var q = "source.sort(order_by=['key1', 'key2'])";
        runner.test("Sort- 2 Cols Descending", q, "key1", "key2", "num1");
    }

    @Test
    public void sort3Cols() {
        var q = "source.sort(order_by=['key1', 'key2', 'key3'])";
        runner.test("Sort- 3 Cols Descending", q, "key1", "key2", "key3", "num1");
    }

}
