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
        runner.tables("source");
        var setup = """
        from deephaven.column import string_col
        where_filter = new_table([
        	string_col("sPrefix", ['s1', 's2', 's3', 's4', 's5', 's6', 's7', 's8', 's9', 's10']),
        	string_col("sSuffix", ['1s', '2s', '3s', '4s', '5s', '6s', '7s', '8s', '9s', '10s'])
        ])
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void whereIn1Filter() {
        var q = "source.where_in(where_filter, cols=['str250 = sPrefix'])";
        runner.test("WhereIn- 1 Filter Col", runner.scaleRowCount, q, "str250", "int250");
    }

    @Test
    public void whereIn2Filter() {
        var q = "source.where_in(where_filter, cols=['str250 = sPrefix', 'str640 = sSuffix'])";
        runner.test("WhereIn- 2 Filter Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

}
