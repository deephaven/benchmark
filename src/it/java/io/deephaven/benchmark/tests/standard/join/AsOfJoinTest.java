/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the aj (As Of-Join) table operation. The first N-1 match columns are exactly matched. The last
 * match column is used to find the key values from the right table that are closest to the values in the left table
 * without going over the left value
 */
public class AsOfJoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source", "right");
    }

    @Test
    void asOfJoinOn1Col() {
        setup(1);
        var q = "source.aj(right, on=['key5 >= r_key5'])";
        runner.test("AsOfJoin- Join On 1 Col", q, "key5", "num1");
    }

    @Test
    void asOfJoinOn2Cols() {
        setup(3);
        var q = "source.aj(right, on=['key1 = r_wild', 'key2 >= r_key2'])";
        runner.test("AsOfJoin- Join On 2 Cols", q, "key1", "key2", "num1");
    }

    @Test
    void asOfJoinOn3Cols() {
        setup(4);
        var q = "source.aj(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 >= r_key1'])";
        runner.test("AsOfJoin- Join On 3 Cols", q, "key1", "key2", "num1");
    }

}
