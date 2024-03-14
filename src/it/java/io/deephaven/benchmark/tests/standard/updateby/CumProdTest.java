/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative product for specified columns and places the
 * result into a new column for each row.
 */
public class CumProdTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import cum_prod");
    }

    @Test
    void cumProd0Group1Col() {
        runner.setScaleFactors(35, 25);
        var q = "timed.update_by(ops=cum_prod(cols=['X1=num1']))";
        runner.test("CumProd- No Groups 1 Col", q, "num1");
    }

    @Test
    void cumProd1Group1Col() {
        runner.setScaleFactors(7, 2);
        var q = "timed.update_by(ops=cum_prod(cols=['X=num1']), by=['key1'])";
        runner.test("CumProd- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void cumProd2Groups1Col() {
        runner.setScaleFactors(2, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X=num1']), by=['key1','key2'])";
        runner.test("CumProd- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void cumProd3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=cum_prod(cols=['X=num1']), by=['key1','key2','key3'])";
        runner.test("CumProd- 2 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
