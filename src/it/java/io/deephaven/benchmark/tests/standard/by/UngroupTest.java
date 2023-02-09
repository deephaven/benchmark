/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. ungroups column content. It is the inverse of the group_by method.
 * Ungroup unwraps columns containing either Deephaven arrays or java arrays.
 * <p/>
 * Note: These test do group then ungroup, since the data generator does not support arrays
 */
public class UngroupTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long scaleRowCount = runner.scaleRowCount;

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
    public void ungroup1Group2Cols() {
        var q = "source.group_by(by=['str1']).ungroup(cols=['int1'])";
        runner.test("Ungroup- 1 Group 250 Unique Vals", scaleRowCount, q, "str1", "int1");
    }

    @Test
    public void ungroup1Group2ColsLarge() {
        var q = "source.group_by(by=['str3']).ungroup(cols=['int3'])";
        runner.test("Ungroup- 1 Group 1M Unique Vals", scaleRowCount, q, "str3", "int3");
    }

    @Test
    public void ungroup2Group3Cols() {
        var q = "source.group_by(by=['str1', 'str2']).ungroup(cols=['int1'])";
        runner.test("Ungroup- 2 Group 160K Unique Combos", scaleRowCount, q, "str1", "str2", "int1");
    }

}
