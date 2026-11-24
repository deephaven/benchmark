/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative maximum for specified columns and places the
 * result into a new column for each row.
 */
public class CumMaxTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import cum_max");
    }

    @Test
    void cumMax0Group1Col() {
        runner.setScaleFactors(120, 100);
        var q = "timed.update_by(ops=cum_max(cols=['X=num1']))";
        runner.test("CumMax- No Groups 1 Col", q, "num1");
    }

    @Test
    void cumMax1Group1Col() {
        runner.setScaleFactors(26, 5);
        var q = "timed.update_by(ops=cum_max(cols=['X=num1']), by=['key1'])";
        runner.test("CumMax- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void cumMax2Groups1Col() {
        runner.setScaleFactors(6.6, 2.6);
        var q = "timed.update_by(ops=cum_max(cols=['X=num1']), by=['key1','key2'])";
        runner.test("CumMax- 2 Groups 10K Unique Combos",  q, "key1", "key2", "num1");
    }
    
    @Test
    void cumMax3Groups1Col() {
        runner.setScaleFactors(2.9, 0.75);
        var q = "timed.update_by(ops=cum_max(cols=['X=num1']), by=['key1','key2','key3'])";
        runner.test("CumMax- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
