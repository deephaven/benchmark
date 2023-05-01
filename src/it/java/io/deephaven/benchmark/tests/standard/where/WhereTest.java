/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.where;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the where table operation. Filters rows of data from the source table.
 */
public class WhereTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void where1Filter() {
        var q = """
        source.where(filters=["str250 = '250'"]);
        """;
        runner.test("Where- 1 Filter", runner.scaleRowCount, q, "str250", "int250");
    }
    
    @Test
    public void where2Filters() {
        var q = """
        source.where(filters=["str250 = '250'", "str640 = '640'"]);
        """;
        runner.test("Where- 2 Filters", runner.scaleRowCount, q, "str250", "str640", "int250");
    }
    
    @Test
    public void whereFilterInList() {
        var q = """
        source.where(filters=["str250 in '250', '1', '249', '2', '248'"]);
        """;
        runner.test("Where- 1 Filter-In", runner.scaleRowCount, q, "str250", "str640", "int250");
    }
    
    @Test
    public void whereOneOf2Filters() {
        var q = """
        source.where_one_of(filters=["str250 = '250'", "str640 = '640'"]);
        """;
        runner.test("WhereOneOf- 2 Filters", runner.scaleRowCount, q, "str250", "str640", "int250");
    }

}
