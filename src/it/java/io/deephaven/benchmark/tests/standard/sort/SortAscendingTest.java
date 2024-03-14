/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the ascending sort table operation. Sorts rows of data from the source table according to the
 * defined columns
 */
public class SortAscendingTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source");
    }

    @Test
    void sort1Col() {
        runner.setScaleFactors(3, 1);
        var q = "source.sort(order_by=['key1'])";
        runner.test("Sort- 1 Col Ascending", q, "key1", "num1");
    }

    @Test
    void sort2Cols() {
        var q = "source.sort(order_by=['key1', 'key2'])";
        runner.test("Sort- 2 Cols Ascending", q, "key1", "key2", "num1");
    }

    @Test
    void sort3Cols() {
        var q = "source.sort(order_by=['key1', 'key2', 'key3'])";
        runner.test("Sort- 3 Cols Ascending", q, "key1", "key2", "key3", "num1");
    }

}
