/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard combo tests for the sort table operation. Sorts rows of data from the source table according to the defined
 * columns. Sort ascending and descending directions in the same operation.
 */
public class SortComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int staticFactor, int incFactor) {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import SortDirection");
        runner.setScaleFactors(staticFactor, incFactor);
    }

    @Test
    void sort2ColsAscendDescend() {
        setup(3, 1);
        var q = "source.sort(order_by=['key1', 'key2'], order=[SortDirection.ASCENDING, SortDirection.DESCENDING])";
        runner.test("Sort- Both Directions 2 Cols", q, "key1", "key2", "num1");
    }

    @Test
    void sort3ColsAscendDescend() {
        setup(3, 1);
        var q = """
        source.sort(order_by=['key1', 'key2', 'key3'], order=[SortDirection.ASCENDING, SortDirection.DESCENDING,
            SortDirection.ASCENDING])
        """;
        runner.test("Sort- Both Directions 3 Cols", q, "key1", "key2", "key3", "num1");
    }

    @Test
    void sort4ColsAscendDescend() {
        setup(2, 1);
        var q = """
        source.sort(order_by=['key1', 'key2', 'key3', 'key4'], order=[SortDirection.ASCENDING, 
            SortDirection.DESCENDING, SortDirection.ASCENDING, SortDirection.DESCENDING])
        """;
        runner.test("Sort- Both Directions 4 Cols", q, "key1", "key2", "key3", "key4", "num1");
    }

}
