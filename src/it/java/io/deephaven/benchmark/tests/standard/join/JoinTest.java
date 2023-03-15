/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the join table operation. The output table contains rows that have matching values in both tables
 */
public class JoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source", "right");
    }

    @Test
    public void joinOn1Col1Match() {
        var q = "source.join(right, on=['str1M=r_str1M'])";
        runner.test("Join- Join On 1 Col 1 Match", runner.scaleRowCount, q, "str1M", "int250");
    }

    @Test
    public void joinOn2ColsAnyMatch() {
        var q = "source.join(right, on=['str1M=r_str1M', 'str250=r_str250'])";
        runner.test("Join- Join On 2 Cols 1 Match", 1000000, q, "str250", "str1M", "int1M", "int250");
    }

    @Test
    public void joinOn3ColsAnyMatch() {
        var q = "source.join(right, on=['str640=r_str640', 'str250=r_str250', 'int1M=r_int1M'])";
        runner.test("Join- Join On 3 Cols Any Match", 1000, q, "str250", "str640", "int250", "int1M");
    }

}
