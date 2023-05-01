/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the where_in table operation. Filters rows of data from the source table where the rows do not
s * match column values in the filter table.
 */
public class WhereNotInTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        var setup = """
        from deephaven.column import string_col
        where_filter = new_table([
        	string_col("sPrefix", ['250', '1', '249', '2', '248']),
        	string_col("sSuffix", ['250', '1', '249', '2', '248'])
        ])
        
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    public void whereNotIn1Filter() {
        var q = "source.where_not_in(where_filter, cols=['str250 = sPrefix'])";
        runner.test("WhereNotIn- 1 Filter Col", runner.scaleRowCount, q, "str250", "int250");
    }

    @Test
    public void whereNotIn2Filter() {
        var q = "source.where_not_in(where_filter, cols=['str250 = sPrefix', 'str640 = sSuffix'])";
        runner.test("WhereNotIn- 2 Filter Cols", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

}
