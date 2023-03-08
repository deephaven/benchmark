/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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

    @BeforeEach
    public void setup() {
        runner.tables("source", "right");
    }

    @Test
    public void asOfJoinOn1Col1Match() {
        var q = "source.aj(right, on=['str1M=r_str1M'])";
        runner.test("AsOfJoin- Join On 1 Col 1 Match", runner.scaleRowCount, q, "str1M", "int250");
    }

    @Test
    public void asOfJoinOn2Cols1Match() {
        var q = "source.aj(right, on=['str1M=r_str1M', 'int1M=r_int1M'])";
        runner.test("AsOfJoin- Join On 2 Cols 1 Match", runner.scaleRowCount, q, "str1M", "int1M", "int250");
    }

    @Test
    public void asOfJoinOn2ColsManyMatch() {
        var q = "source.aj(right, on=['str640=r_str640', 'str250=r_str250'])";
        runner.test("AsOfJoin- Join On 2 Cols Many Match", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

}
