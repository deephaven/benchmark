/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to <code>EmaTimeTest</code>
 */
public class EmaTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import ema_tick");
    }

    @Test
    void emaTick0Group1Col() {
        runner.setScaleFactors(25, 15);
        var q = "timed.update_by(ops=ema_tick(decay_ticks=5000,cols=['X=num1']))";
        runner.test("EmaTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emaTick1Group1Col() {
        runner.setScaleFactors(6, 1);
        var q = "timed.update_by(ops=ema_tick(decay_ticks=5000,cols=['X=num1']), by=['key1'])";
        runner.test("EmaTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void emaTick2Group1Col() {
        runner.setScaleFactors(5, 1);
        var q = "timed.update_by(ops=ema_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2'])";
        runner.test("EmaTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emaTick3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=ema_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2','key3'])";
        runner.test("EmaTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
