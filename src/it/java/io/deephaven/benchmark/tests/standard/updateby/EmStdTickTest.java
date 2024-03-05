/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving standard deviation for
 * specified columns and places the result into a new column for each row. *
 * <p/>
 * Note: This test must contain benchmarks and <code>decay_ticks</code> that are comparable to
 * <code>EmStdTimeTest</code>
 */
public class EmStdTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        runner.addSetupQuery("from deephaven.updateby import emstd_tick");
    }

    @Test
    void emStdTick0Group1Col() {
        runner.setScaleFactors(15, 8);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=5000,cols=['X=num1']))";
        runner.test("EmStdTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void emStdTick1Group1Col() {
        runner.setScaleFactors(6, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=5000,cols=['X=num1']), by=['key1'])";
        runner.test("EmStdTick- 1 Group 100 Unique Vals 1 Col", q, "key1", "num1");
    }

    @Test
    void emStdTick2Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2'])";
        runner.test("EmStdTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void emStdTick3Groups1Col() {
        runner.setScaleFactors(1, 1);
        var q = "timed.update_by(ops=emstd_tick(decay_ticks=5000,cols=['X=num1']), by=['key1','key2','key3'])";
        runner.test("EmStdTick- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
