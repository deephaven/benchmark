/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the raj (Reverse As Of-Join) table operation. The first N-1 match columns are exactly matched. The
 * last match column is used to find the key values from the right table that are closest to the values in the left
 * table without going under the left value
 */
public class ReverseAsOfJoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(2);
        runner.tables("source", "right");
    }

    @Test
    public void reverseAsOfJoinOn1Col1Match() {
        var q = "source.raj(right, on=['str1M=r_str1M'])";
        runner.test("ReverseAsOfJoin- Join On 1 Col 1 Match",  q, "str1M", "int250");
    }

    @Test
    public void reverseAsOfJoinOn2Cols1Match() {
        var q = "source.raj(right, on=['str1M=r_str1M', 'int1M=r_int1M'])";
        runner.test("ReverseAsOfJoin- Join On 2 Cols 1 Match", q, "str1M", "int1M", "int250");
    }

    @Test
    public void reverseAsOfJoinOn2ColsManyMatch() {
        var q = "source.raj(right, on=['str640=r_str640', 'str250=r_str250'])";
        runner.test("ReverseAsOfJoin- Join On 2 Cols Many Match", q, "str250", "str640",
                "int250");
    }

}
