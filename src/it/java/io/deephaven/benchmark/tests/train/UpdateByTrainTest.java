/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import org.junit.jupiter.api.*;

/**
 * Standard tests for the updateBy table operation. Combines a mixture of rolling operations and cumulative operations
 */
public class UpdateByTrainTest {
    final TrainTestRunner runner = new TrainTestRunner(this);
    final String noGroups = """
        avg_contains = rolling_avg_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time='PT5S',fwd_time='PT5S')
        max_before = rolling_max_tick(cols=['C=num1','D=num2'], rev_ticks=3000,fwd_ticks=-1000)
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time='-PT1S',fwd_time='PT4S')
        """;
    final String group10K = """
        avg_contains = rolling_avg_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time='PT4M',fwd_time='PT5M')
        max_before = rolling_max_tick(cols=['C=num1','D=num2'], rev_ticks=30,fwd_ticks=-10)
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time='-PT1M',fwd_time='PT4M')
        """;

    void setup(double staticRowFactor, double incRowFactor) {
        runner.tables(staticRowFactor, incRowFactor, "timed");
        var setup = """
        from deephaven.updateby import rolling_avg_time, rolling_max_tick, rolling_prod_time
        from deephaven.updateby import ema_tick, cum_min, cum_sum

        ema_tick_op = ema_tick(decay_ticks=10000,cols=['G=num1','H=num2'])
        min_op = cum_min(cols=['I=num1','J=num2'])
        sum_op = cum_sum(cols=['K=num1','L=num2'])
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    @Disabled
    void mixedComboNoGroups() {
        setup(21.8, 17);
        runner.addSetupQuery(noGroups);
        var q = "timed.update_by(ops=[avg_contains, max_before, prod_after, ema_tick_op, min_op, sum_op])";
        runner.test("UpdateBy- No Groups 12 Cols", 0, q, "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo2Groups() {
        setup(9, 0.2);  // 0.2 for 100ms, 4.5 for 1s
        runner.addSetupQuery(group10K);
        var q = """
        timed.update_by(ops=[avg_contains,max_before,prod_after,ema_tick_op,min_op,sum_op], by=['key1','key2'])
        """;
        runner.test("UpdateBy- 2 Groups 10K Unique Combos", 0, q, "key1", "key2", "num1", "num2", "timestamp");
    }

}
