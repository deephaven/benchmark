/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that could be shown to customers
 */
public class WhereTest {
    final ExperimentalTestRunner runner = new ExperimentalTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int500", "int", "[1-500]")
                .add("int1K", "int", "[1-1000]")
                .add("str10K", "string", "s[1-10000]v")
                .generateParquet();
    }
    
    @Test
    public void whereIntCompare() {
        var q = "source.where(filters=['int500 == int1K'])";
        runner.test("Where- Int Compare", 10000, q, "int1K", "int500");
    }
    
    @Test
    public void whereStringCompare() {
        var q = "source.where(filters=[\"str10K == 's10v'\"])";
        runner.test("Where- String Compare", 10000, q, "str10K", "int500");
    }
    
    @Test
    public void whereIntAndStringCompare() {
        var q = "source.where(filters=['int500 < int1K', \"str10K == 's1v'\"])";
        runner.test("Where- Int And String Compare", 10000, q, "str10K", "int500", "int1K");
    }

}
