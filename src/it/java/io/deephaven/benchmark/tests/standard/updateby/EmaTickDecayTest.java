/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 */
public class EmaTickDecayTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int5", "int", "[1-5]")
                .add("int10", "int", "[1-10]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        runner.api().query("from deephaven.updateby import ema_tick_decay").execute();
    }

    @Test
    public void emaTickDecay0Group1Col() {
        var q = "source.update_by(ops=ema_tick_decay(time_scale_ticks=100,cols=['X=int5']))";
        runner.test("EmaTickDecay- No Groups 1 Col", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void emaTickDecay0Group2Cols() {
        var q = "source.update_by(ops=ema_tick_decay(time_scale_ticks=100,cols=['X=int5','Y=int10']))";
        runner.test("EmaTickDecay- No Groups 2 Cols", runner.scaleRowCount, q, "int5", "int10");
    }

    @Test
    public void emaTickDecay1Group2Cols() {
        var q = "source.update_by(ops=ema_tick_decay(time_scale_ticks=100,cols=['X=int5']), by=['str100'])";
        runner.test("EmaTickDecay- 1 Group 100 Unique Vals 2 Col", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void emaTickDecay1Group3Cols() {
        var q = "source.update_by(ops=ema_tick_decay(time_scale_ticks=100,cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("EmaTickDecay- 1 Group 100 Unique Vals 3 Cols", runner.scaleRowCount, q, "str100", "int5", "int10");
    }

    @Test
    public void emaTickDecay2Groups3Cols() {
        var q = "source.update_by(ops=ema_tick_decay(time_scale_ticks=100,cols=['X=int5']), by=['str100','str150'])";
        runner.test("EmaTickDecay- 2 Groups 160K Unique Combos 3 Cols", runner.scaleRowCount, q, "str100", "str150",
                "int5");
    }

}
