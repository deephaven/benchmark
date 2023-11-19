/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the where_not_in table operation. Filters rows of data from the source table where the rows match
 * column values in the filter table.
 */
public class WhereInTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
        var setup = """
        from deephaven.column import string_col
        where_filter = new_table([
        	string_col("sPrefix", ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10']),
        	string_col("sSuffix", ['1', '2', '3', '4', '5', '6', '7', '8', '9', '10'])
        ])
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void whereIn1Filter() {
        runner.setScaleFactors(210, 150);
        var q = "source.where_in(where_filter, cols=['str250 = sPrefix'])";
        runner.test("WhereIn- 1 Filter Col", q, "str250", "int250");
    }

    @Test
    public void whereIn2Filter() {
        runner.setScaleFactors(85, 60);
        var q = "source.where_in(where_filter, cols=['str250 = sPrefix', 'str640 = sSuffix'])";
        runner.test("WhereIn- 2 Filter Cols", q, "str250", "str640", "int250");
    }

}
