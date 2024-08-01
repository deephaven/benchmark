/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the whereOneOf table operation. Filters rows of data from the source table where the rows match
 * column values in the filter table.
 */
@Tag("Iterate")
public class WhereOneOfTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    void whereOneOf1Filter() {
        runner.setScaleFactors(365, 0);
        var q = """
        source.where_one_of(filters=["key1 = '50'"]);
        """;
        runner.test("WhereOneOf- 1 Filter", q, "key1", "num1");
    }

    @Test
    void whereOneOf2Filters() {
        runner.setScaleFactors(90, 80);
        var q = """
        source.where_one_of(filters=["key1 = '50'", "key2 = '51'"]);
        """;
        runner.test("WhereOneOf- 2 Filters", q, "key1", "key2", "num1");
    }
//
//    @Test
//    void whereOneOf3Filters() {
//        runner.setScaleFactors(40, 40);
//        var q = """
//        source.where_one_of(filters=["key1 = '50'", "key2 = '51'", "key3 in -2, -1, 0, 1, 2"]);
//        """;
//        runner.test("WhereOneOf- 3 Filters", q, "key1", "key2", "key3", "num1");
//    }

}
