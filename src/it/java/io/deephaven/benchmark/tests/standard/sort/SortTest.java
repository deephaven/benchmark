/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the sort table operation. Sorts rows of data from the source table according to the defined
 * columns
 */
public class SortTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        runner.addSetupQuery("from deephaven import SortDirection");
    }

    @Test
    public void sort1Col() {
        var q = "source.sort(order_by=['str250'])";
        runner.test("Sort- 1 Col", runner.scaleRowCount, q, "str250", "int250");
    }

    @Test
    public void sort2Cols() {
        var q = "source.sort(order_by=['str250', 'str640'])";
        runner.test("Sort- 2 Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

    @Test
    public void sort2ColsOppositeOrder() {
        var q = "source.sort(order_by=['str250', 'str640'], order=[SortDirection.ASCENDING, SortDirection.DESCENDING])";
        runner.test("Sort- 2 Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

    @Test
    public void sortDescending2Cols() {
        var q = "source.sort_descending(order_by=['str250', 'str640'])";
        runner.test("SortDescending- 2 Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }
    
    @Test
    public void reverse3Cols() {
        var q = "source.reverse()";
        runner.test("Reverse- 3 Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

}
