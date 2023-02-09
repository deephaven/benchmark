/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. Groups column content into arrays.
 */
public class GroupByTest {
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
    public void groupBy0Groups3Cols() {
        var q = "source.group_by()";
        runner.test("GroupBy- No Groups 3 Cols", 1, q, "str1", "str2", "int1");
    }

    @Test
    public void groupBy1Group2Cols() {
        var q = "source.group_by(by=['str1'])";
        runner.test("GroupBy- 1 Group 250 Unique Vals", 250, q, "str1", "int1");
    }

    @Test
    public void groupBy1Group2ColsLarge() {
        var q = "source.group_by(by=['str3'])";
        runner.test("GroupBy- 1 Group 1M Unique Vals", 1000000, q, "str3", "int3");
    }

    @Test
    public void groupBy2Group3Cols() {
        var q = "source.group_by(by=['str1', 'str2'])";
        runner.test("GroupBy- 2 Group 160K Unique Combos", 160000, q, "str1", "str2", "int1");
    }

}
