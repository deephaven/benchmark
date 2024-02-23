/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source", "right");
    }

    @Test
    public void joinOn1Col1Match() {
        var q = "source.join(right, on=['str1M=r_str1M'])";
        runner.test("Join- Join On 1 Col 1 Match", q, "str1M", "int250");
    }

    @Test
    public void joinOn2ColsAnyMatch() {
        var q = "source.join(right, on=['str1M=r_str1M', 'str250=r_str250'])";
        runner.test("Join- Join On 2 Cols 1 Match", q, "str250", "str1M", "int1M", "int250");
    }

    @Test
    public void joinOn3ColsAnyMatch() {
        var q = "source.join(right, on=['str640=r_str640', 'str250=r_str250', 'int1M=r_int1M'])";
        runner.test("Join- Join On 3 Cols Any Match", q, "str250", "str640", "int250", "int1M");
    }

}
