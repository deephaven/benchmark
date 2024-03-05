/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines a mixture of rolling operations and cumulative operations
 */
public class MixedComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    String setupStr = null;

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        setupStr = """
        from deephaven.updateby import rolling_avg_time, rolling_max_tick, rolling_prod_time
        from deephaven.updateby import ema_tick, cum_min, cum_sum
         
        avg_contains = rolling_avg_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time='PT5S',fwd_time='PT5S')
        max_before = rolling_max_tick(cols=['C=num1','D=num2'], rev_ticks=3000,fwd_ticks=-1000)
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time='-PT1S',fwd_time='PT4S')
        
        ema_tick_op = ema_tick(decay_ticks=10000,cols=['G=num1','H=num2'])
        min_op = cum_min(cols=['I=num1','J=num2'])
        sum_op = cum_sum(cols=['K=num1','L=num2'])
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void mixedComboNoGroups6Ops() {
        var q = "timed.update_by(ops=[avg_contains, max_before, prod_after, ema_tick_op, min_op, sum_op])";
        runner.test("MixedCombo- 6 Ops No Groups", q, "num1", "num2", "timestamp");
    }

    @Test
    void mixedCombo1Group6Ops() {
        var q = """
        timed.update_by(ops=[avg_contains,max_before,prod_after,ema_tick_op,min_op,sum_op], by=['key1'])
        """;
        runner.test("MixedCombo- 6 Ops 1 Groups 100 Unique Vals", q, "key1", "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo2Groups6Ops() {
        var q = """
        timed.update_by(ops=[avg_contains,max_before,prod_after,ema_tick_op,min_op,sum_op], by=['key1','key2'])
        """;
        runner.test("MixedCombo- 6 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2",
                "timestamp");
    }

    @Test
    void rollingCombo3Groups6Ops() {
        var q = """
        timed.update_by(ops=[avg_contains,max_before,prod_after,ema_tick_op,min_op,sum_op], 
            by=['key1','key2','key3'])
        """;
        runner.test("MixedCombo- 6 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "num2",
                "timestamp");
    }

    @Test
    @Disabled
    void rollingCombo3Groups6OpsLarge() {
        var q = """
        timed.update_by(ops=[avg_contains,max_before,prod_after,ema_tick_op,min_op,sum_op], 
            by=['key1','key2','int103'])
        """;
        runner.test("MixedCombo- 6 Ops 3 Groups 1M Unique Combos", q, "key1", "key2", "key4", "num1", "num2",
                "timestamp");
    }

}
