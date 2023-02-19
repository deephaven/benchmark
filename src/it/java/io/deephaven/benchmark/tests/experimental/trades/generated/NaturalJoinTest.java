/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that could be shown to customers
 */
public class NaturalJoinTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int1K", "int", "[1-1000]")
                .add("int10K", "int", "[1-10000]")
                .add("str10K", "string", "s[1-10000]v")
                .generateParquet();

        runner.api().table("right").fixed()
                .add("int10Kr", "int", "[1-10000]")
                .add("str10Kr", "string", "s[1-10000]v")
                .add("str10Kr2", "string", "s[10001-20000]v")
                .generateParquet();
        runner.addSupportTable("right");
    }

    @Test
    public void naturalJoin1StringCompare() {
        var q = "source.natural_join(table=right, on=['str10K=str10Kr'])";
        runner.test("NaturalJoin- Join On 1 String", runner.getScaleRowCount(), q, "str10K", "int1K");
    }
    
    @Test
    public void naturalJoin1IntCompare() {
        var q = "source.natural_join(table=right, on=['int10K=int10Kr'])";
        runner.test("NaturalJoin- Join On 1 Int", runner.getScaleRowCount(), q, "int1K", "int10K", "str10K");
    }
    
    @Test
    public void naturalJoin2IntAndStringCompare() {
        var q = "source.natural_join(table=right, on=['int10K=int10Kr', 'str10K=str10Kr'])";
        runner.test("NaturalJoin- Join On 2 Cols", runner.getScaleRowCount(), q, "int1K", "int10K", "str10K");
    }

}
