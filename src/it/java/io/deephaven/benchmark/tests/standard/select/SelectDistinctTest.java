/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.select;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the selectDistinct table operation. Creates a new table containing all of the unique values for a set of key columns
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
        var q = "source.select_distinct(formulas=['key1'])";
        runner.test("SelectDistinct- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    public void selectDistict2Groups() {
        runner.setScaleFactors(4, 4);
        var q = "source.select_distinct(formulas=['key1', 'key2'])";
        runner.test("SelectDistinct- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }
    
    @Test
    public void selectDistict3Groups() {
        runner.setScaleFactors(4, 4);
        var q = "source.select_distinct(formulas=['key1', 'key2', 'key3'])";
        runner.test("SelectDistinct- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
