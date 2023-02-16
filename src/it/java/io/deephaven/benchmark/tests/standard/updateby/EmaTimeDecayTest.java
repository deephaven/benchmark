/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a tick-based exponential moving average for specified
 * columns and places the result into a new column for each row.
 */
public class EmaTimeDecayTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final long rowCount = runner.scaleRowCount;

    @BeforeEach
    public void setup() {
        long baseTime = 1676557157537L;
        runner.api().table("source").fixed()
                .add("timestamp", "timestamp-millis", "[" + baseTime + "-" + (baseTime + rowCount - 1) + "]")
                .add("intScale", "int", "[1-" + (rowCount - 1) + "]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        runner.api().query("from deephaven.updateby import ema_time_decay").execute();
    }

    @Test
    public void emaTimeDecay1Group3Cols() {
        var q = "source.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=intScale']), by=['str100'])";
        runner.test("EmaTimeDecay- 1 Group 100 Unique Vals 2 Col", rowCount, q, "str100", "intScale", "timestamp");
    }

    @Test
    public void emaTimeDecay2Groups4Cols() {
        var q = "source.update_by(ops=ema_time_decay(ts_col='timestamp', time_scale='00:00:02', cols=['X=intScale']), by=['str100','str150'])";
        runner.test("EmaTickDecay- 2 Groups 160K Unique Combos 3 Cols", runner.scaleRowCount, q, "str100", "str150",
                "intScale", "timestamp");
    }

}
