/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the natural_join table operation. Column values will be added to each left row from exactly one
 * matched row from the right table or null values if no match
 */
public class NaturalJoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source", "right");
    }

    @Test
    public void NaturalJoinOn1Col1Match() {
        var q = "source.natural_join(right, on=['str1M=r_str1M'])";
        runner.test("NaturalJoin- Join On 1 Col 1 Match", q, "str1M", "int250");
    }

    @Test
    public void NaturalJoinOn2Cols1Match() {
        var q = "source.natural_join(right, on=['str1M=r_str1M', 'int1M=r_int1M'])";
        runner.test("NaturalJoin- Join On 2 Cols 1 Match", q, "str1M", "int1M", "int250");
    }

}
