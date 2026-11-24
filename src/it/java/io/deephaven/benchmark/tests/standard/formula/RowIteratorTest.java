/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for iterating through tables to access column values directly. These benchmarks iterate through the
 * same columns and do the same sums.
 */
public class RowIteratorTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source");
    }

    @Test
    void iterDict2Cols() {
        runner.setScaleFactors(1.9, 0);
        var q = """
        new_table([
            double_col('total', [sum(row['num1'] + row['num2'] for row in source.iter_dict())])
        ])
        """;
        runner.test("Row-IterDict- Sum 2 Double Cols", 1, q, "num1", "num2");
    }

    @Test
    void iterTuple2Cols() {
        runner.setScaleFactors(2.7, 0);
        var q = """
        new_table([
            double_col('total', [sum(row.num1 + row.num2 for row in source.iter_tuple())])
        ])
        """;
        runner.test("Row-IterTuple- Sum 2 Double Cols", 1, q, "num1", "num2");
    }
}
