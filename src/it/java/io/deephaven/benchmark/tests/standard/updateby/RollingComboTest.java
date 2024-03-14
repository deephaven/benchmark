/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines multiple rolling operations for tick and time windows
 */
public class RollingComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    String setupStr = null;

    @BeforeEach
    void setup() {
        runner.setRowFactor(1);
        runner.tables("timed");
        setupStr = """
        from deephaven.updateby import rolling_sum_time, rolling_min_time, rolling_prod_time
        from deephaven.updateby import rolling_avg_tick, rolling_max_tick, rolling_group_tick
         
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time='PT5S',fwd_time='PT5S')
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time='PT3S',fwd_time=int(-1e9))
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time='-PT1S',fwd_time=int(3e9))
        
        avg_contains = rolling_avg_tick(cols=['G=num1','H=num2'], rev_ticks=5000, fwd_ticks=5000)
        max_before = rolling_max_tick(cols=['I=num1','J=num2'], rev_ticks=3000, fwd_ticks=-1000)
        group_after = rolling_group_tick(cols=['K=num1','L=num2'], rev_ticks=-1000, fwd_ticks=3000)
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void rollingCombo0Groups6Ops() {
        var q = "timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after])";
        runner.test("RollingCombo- 6 Ops No Groups", q, "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo1Groups6Ops() {
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], by=['key1']);
        """;
        runner.test("RollingCombo- 6 Ops 1 Groups 100 Unique Vals", q, "key1", "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo2Groups6Ops() {
        var q = """
        timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after], 
            by=['key1','key2']);
        """;
        runner.test("RollingCombo- 6 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2",
                "timestamp");
    }

    @Test
    void rollingCombo3Groups6Ops() {
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], 
            by=['key1','key2','key3']);
        """;
        runner.test("RollingCombo- 6 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "num2",
                "timestamp");
    }

    @Test
    void rollingCombo3Groups6OpsLarge() {
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], 
            by=['key1','key2','key4']);
        """;
        runner.test("RollingCombo- 6 Ops 3 Groups 1M Unique Combos", q, "key1", "key2", "key4", "num1", "num2",
                "timestamp");
    }

}
