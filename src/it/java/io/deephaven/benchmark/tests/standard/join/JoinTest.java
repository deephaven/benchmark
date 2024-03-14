/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the join table operation. The output table contains all of the rows and columns of the left table
 * plus additional columns containing data from the right table. For columns appended to the left table, row values
 * equal the row values from the right table where the key values in the left and right tables are equal. If there is no
 * matching key in the right table, appended row values are NULL. If there are multiple matches, the operation will
 * fail.
 */
public class JoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source", "right");
    }

    @Test
    void joinOn1Col() {
        setup(2);
        var q = "source.join(right, on=['key5 = r_key5'])";
        runner.test("Join- Join On 1 Col", q, "key5", "num1");
    }

    @Test
    void joinOn2Cols() {
        setup(3);
        var q = "source.join(right, on=['key1 = r_wild', 'key2 = r_key2'])";
        runner.test("Join- Join On 2 Cols", q, "key1", "key2", "num1");
    }

    @Test
    void joinOn3Cols() {
        setup(3);
        var q = "source.join(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 = r_key1'])";
        runner.test("Join- Join On 3 Cols", q, "key1", "key2", "num1");
    }

}
