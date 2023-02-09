/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the headBy table operation. Returns the first n rows for each group.
 */
public class HeadByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int1", "int", "[1-250]")
                .add("int3", "int", "[1-1000000]")
                .add("str1", "string", "string[1-250]val")
                .add("str2", "string", "val[1-640]string")
                .add("str3", "string", "val[1-1000000]string")
                .generateParquet();
    }

    @Test
    public void headBy1Group2Cols() {
        var q = "source.head_by(2, by=['str1'])";
        runner.test("HeadBy- 1 Group 250 Unique Vals 2 Rows Per", 250 * 2, q, "str1", "int1");
    }

    @Test
    public void headBy1Group2ColsLarge() {
        var q = "source.head_by(2, by=['str3'])";
        runner.test("HeadBy- 1 Group 1M Unique Vals 2 Rows Per", 1000000 * 2, q, "str3", "int3");
    }

    @Test
    public void headBy2Group3Cols() {
        var q = "source.head_by(2, by=['str1', 'str2'])";
        runner.test("LastBy- 2 Group 160K Unique Combos 2 Rows Per", 160000 * 2, q, "str1", "str2", "int1");
    }

}
