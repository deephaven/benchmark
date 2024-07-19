/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for iterating through tables to access column values directly. These benchmarks iterate through the
 * same columns and do the same sums.
 */
public class ColumnIterationTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(1, 0);
    }

    @Test
    void iterDict2Cols() {
        setup(2);
        var q = """
        new_table([
            double_col('total', [sum(row['num1'] + row['num2'] for row in source.iter_dict())])
        ])
        """;
        runner.test("IterDict- Sum 2 Double Cols", 1, q, "num1", "num2");
    }

    @Test
    void iterTuple2Cols() {
        setup(4);
        var q = """
        new_table([
            double_col('total', [sum(row.num1 + row.num2 for row in source.iter_tuple())])
        ])
        """;
        runner.test("IterTuple- Sum 2 Double Cols", 1, q, "num1", "num2");
    }

}
