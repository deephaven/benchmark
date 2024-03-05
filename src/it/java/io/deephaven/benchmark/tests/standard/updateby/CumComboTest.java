/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines multiple rolling operations.
 */
public class CumComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    String setupStr = null;

    @BeforeEach
    void setup() {
        runner.setRowFactor(5);
        runner.tables("timed");

        setupStr = """
        from deephaven.updateby import ema_tick, ema_time
        from deephaven.updateby import cum_max, cum_min, cum_sum, cum_prod
        
        ema_tick_op = ema_tick(decay_ticks=100,cols=['A=num1','B=num2'])
        ema_time_op = ema_time(ts_col='timestamp', decay_time='PT10S', cols=['C=num1','D=num2'])
        max_op = cum_max(cols=['E=num1','F=num2'])
        min_op = cum_min(cols=['G=num1','H=num2'])
        sum_op = cum_sum(cols=['I=num1','J=num2'])
        prod_op = cum_prod(cols=['K=num1','L=num2'])
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void cumComboNoGroups6Ops() {
        runner.setScaleFactors(6, 3);
        var q = "timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op])";
        runner.test("CumCombo- 6 Ops No Groups", q, "num1", "num2", "timestamp");
    }

    @Test
    void cumCombo1Groups6Ops() {
        var q = """
        timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op], by=['key1'])
        """;
        runner.test("CumCombo- 6 Ops 1 Group 100 Unique Combos", q, "key1", "num1", "num2", "timestamp");
    }

    @Test
    void cumCombo2Groups6Ops() {
        var q = """
        timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op], by=['key1','key2'])
        """;
        runner.test("CumCombo- 6 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2", "timestamp");
    }

    @Test
    void cumCombo3Groups6Ops() {
        var q = """
        timed.update_by(ops=[ema_tick_op,ema_time_op,max_op,min_op,sum_op,prod_op], by=['key1','key2','key3'])
        """;
        runner.test("CumCombo- 6 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "num2",
                "timestamp");
    }

    @Test
    @Disabled
    void cumCombo3Groups6OpsLarge() {
        var q = """
        timed.update_by(ops=[ema_tick_op,ema_time_op,max_op,min_op,sum_op,prod_op], by=['key1','key2','key4'])
        """;
        runner.test("CumCombo- 6 Ops 3 Groups 1M Unique Combos", q, "key1", "key2", "key4", "num1", "num2",
                "timestamp");
    }

}
