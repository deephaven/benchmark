/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving maximum for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to
 * <code>EmMaxTimeTest</code>
 */
public class EmMaxTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(3);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emmax_tick");
        
    }

    @Test
    void emMaxTick0Group1Col() {
        runner.setScaleFactors(30, 25);
        var q = "timed.update_by(ops=emmax_tick(decay_ticks=5000,cols=['X=num1']))";
        runner.test("EmMaxTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emMaxTick1Group1Col() {
        runner.setScaleFactors(9, 2);
        var q = "timed.update_by(ops=emmax_tick(decay_ticks=5000,cols=['X=num1']), by=['key1'])";
        runner.test("EmMaxTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void emMaxTick2Groups1Col() {
        runner.setScaleFactors(2, 1);
        var q = "timed.update_by(ops=emmax_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2'])";
        runner.test("EmMaxTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emMaxTick3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=emmax_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2','key3'])";
        runner.test("EmMaxTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
