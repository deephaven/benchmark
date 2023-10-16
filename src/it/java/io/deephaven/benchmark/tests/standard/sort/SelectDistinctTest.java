/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.sort;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the select_distinct table operation. Creates a new table containing all of the unique values for a set of key columns
 */
public class SelectDistinctTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void selectDistict1Group() {
        runner.setScaleFactors(20, 20);
        var q = "source.select_distinct(formulas=['str250'])";
        runner.test("SelectDistinct- 1 Group 250 Unique Vals", 250, q, "str250", "str640", "int640");
    }

    @Test
    public void selectDistict2Group() {
        runner.setScaleFactors(4, 4);
        var q = "source.select_distinct(formulas=['str250', 'str640'])";
        runner.test("SelectDistinct- 1 Group 160K Unique Vals", 160000, q, "str250", "str640", "int640");
    }

}
