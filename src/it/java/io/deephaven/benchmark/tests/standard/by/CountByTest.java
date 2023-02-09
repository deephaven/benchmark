/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the countBy table operation. Returns the number of rows for each group.
 */
public class CountByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int1", "int", "[1-250]")
                .add("int2", "int", "[1-640]")
                .add("int3", "int", "[1-1000000]")
                .add("str1", "string", "string[1-250]val")
                .add("str2", "string", "val[1-640]string")
                .generateParquet();
    }

    @Test
    public void countBy0Groups3Cols() {
        var q = "source.count_by('count')";
        runner.test("CountBy- No Groups 250 Unique Vals", 1, q, "str1", "str2", "int1");
    }

    @Test
    public void countBy1IntGroup1Col() {
        var q = "source.count_by('count', by=['int1'])";
        runner.test("CountBy- 1 Int Group 250 Unique Vals", 250, q, "int1");
    }

    @Test
    public void countBy1IntGroup1ColLarge() {
        var q = "source.count_by('count', by=['int3'])";
        runner.test("CountBy- 1 Int Group 1M Unique Vals", 1000000, q, "int3");
    }

    @Test
    public void countBy1StringGroup1Col() {
        var q = "source.count_by('count', by=['str1'])";
        runner.test("CountBy- 1 String Group 250 Unique Vals", 250, q, "str1");
    }

    @Test
    public void countBy2IntGroups2Cols() {
        var q = "source.count_by('count', by=['int1', 'int2'])";
        runner.test("CountBy- 2 Int Groups 160K Unique Combos", 160000, q, "int1", "int2");
    }

    @Test
    public void countBy2StringGroups2Cols() {
        var q = "source.count_by('count', by=['str1', 'str2'])";
        runner.test("CountBy- 2 String Groups 160K Unique Combos", 160000, q, "str1", "str2");
    }

}
