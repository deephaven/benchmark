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

    @BeforeEach
    void setup() {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import SortDirection");
    }

    @Test
    void sort2ColsAscendDescend() {
        runner.setScaleFactors(7.2, 0.95);
        var q = "source.sort(order_by=['key1', 'key2'], order=[SortDirection.ASCENDING, SortDirection.DESCENDING])";
        runner.test("Sort- Both Directions 2 Cols", q, "key1", "key2", "num1");
    }

    @Test
    void sort3ColsAscendDescend() {
        runner.setScaleFactors(3.2, 0.55);
        var q = """
        source.sort(order_by=['key1', 'key2', 'key3'], order=[SortDirection.ASCENDING, SortDirection.DESCENDING,
            SortDirection.ASCENDING])
        """;
        runner.test("Sort- Both Directions 3 Cols", q, "key1", "key2", "key3", "num1");
    }

    @Test
    void sort4ColsAscendDescend() {
        runner.setScaleFactors(2.5, 0.4);
        var q = """
        source.sort(order_by=['key1', 'key2', 'key3', 'key4'], order=[SortDirection.ASCENDING, 
            SortDirection.DESCENDING, SortDirection.ASCENDING, SortDirection.DESCENDING])
        """;
        runner.test("Sort- Both Directions 4 Cols", q, "key1", "key2", "key3", "key4", "num1");
    }

}
