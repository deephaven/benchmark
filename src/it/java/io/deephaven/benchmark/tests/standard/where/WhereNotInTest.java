/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the whereNotIn table operation. Filters rows of data from the source table where the rows match
 * column values in the filter table. Note: These benchmarks do the converse of the ones in WhereInTest
 */
public class WhereNotInTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
        var setup = """
        from deephaven.column import string_col, int_col
        where_filter = new_table([
            string_col("set1", ['1', '2', '3', '4', '5', '6', '7', '8']),
            string_col("set2", ['10', '20', '30', '40', '50', '60', '70', '80']),
            int_col("set3", [-1, -2, -3, -4, 5, 6, 7, 8])
        ])
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    @Tag("Iterate")
    void whereNotIn1Filter() {
        runner.setScaleFactors(80, 75);
        var q = "source.where_not_in(where_filter, cols=['key1 = set1'])";
        runner.test("WhereNotIn- 1 Filter Col", q, "key1", "num1");
    }

    @Test
    void whereNotIn2Filter() {
        runner.setScaleFactors(75, 65);
        var q = "source.where_not_in(where_filter, cols=['key1 = set1', 'key2 = set2'])";
        runner.test("WhereNotIn- 2 Filter Cols", q, "key1", "key2", "num1");
    }

    @Test
    void whereNotIn3Filter() {
        runner.setScaleFactors(55, 50);
        var q = "source.where_not_in(where_filter, cols=['key1 = set1', 'key2 = set2', 'key3 = set3'])";
        runner.test("WhereNotIn- 3 Filter Cols", q, "key1", "key2", "key3", "num1");
    }

}
