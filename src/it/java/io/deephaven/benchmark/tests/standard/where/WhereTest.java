/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the where table operation. Filters rows of data from the source table.
 */
public class WhereTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    @Tag("Iterate")
    void where1Filter() {
        runner.setScaleFactors(330, 0);
        var q = """
        source.where(filters=["key1 = '50'"])
        """;
        runner.test("Where- 1 Filter", q, "key1", "num1");
    }

//    @Test
//    void where2Filters() {
//        runner.setScaleFactors(310, 300);
//        var q = """
//        source.where(filters=["key1 = '50'", "key2 = '51'"])
//        """;
//        runner.test("Where- 2 Filters", q, "key1", "key2", "num1");
//    }
//
//    @Test
//    void where3Filters() {
//        runner.setScaleFactors(320, 290);
//        var q = """
//        source.where(filters=["key1 = '50'", "key2 = '51'", "key3 in -2, -1, 0, 1, 2"])
//        """;
//        runner.test("Where- 3 Filters", q, "key1", "key2", "key3", "num1");
//    }

}
