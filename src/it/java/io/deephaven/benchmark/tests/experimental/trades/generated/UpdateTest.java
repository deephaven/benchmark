/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.experimental.trades.generated;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.experimental.ExperimentalTestRunner;

/**
 * Basic where tests that could be shown to customers
 */
public class UpdateTest {
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
    public void update1IntAdd2Cols() {
        var q = "source.update(formulas=['X = int500 + int1K'])";
        runner.test("Update- 1 Int Add 2 Cols", runner.getScaleRowCount(), q, "int500", "int1K");
    }
    
    @Test
    public void update2IntAdd2Cols() {
        var q = "source.update(formulas=['X = int500 + int1K', 'Y = int1K - int500'])";
        runner.test("Update- 2 Ints Add 2 Cols", runner.getScaleRowCount(), q, "int500", "int1K");
    }

}
