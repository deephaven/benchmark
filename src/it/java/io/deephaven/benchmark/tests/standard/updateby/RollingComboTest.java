/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines multiple rolling operations for tick and time windows
 */
public class RollingComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);
    final String noGroups = """
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time="PT2S",fwd_time="PT3S")
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time="PT2S",fwd_time="-PT1S")
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time="-PT1S",fwd_time="PT2S")
        
        avg_contains = rolling_avg_tick(cols=['G=num1','H=num2'], rev_ticks=2000, fwd_ticks=3000)
        max_before = rolling_max_tick(cols=['I=num1','J=num2'], rev_ticks=2000, fwd_ticks=-1000)
        group_after = rolling_group_tick(cols=['K=num1','L=num2'], rev_ticks=-1000, fwd_ticks=2000)
        """;
    final String group100 = """
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time="PT2S",fwd_time="PT3S")
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time="PT2S",fwd_time="-PT1S")
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time="-PT1S",fwd_time="PT2S")
        """;
    final String group10K = """
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time="PT4M",fwd_time="PT5M")
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time="PT3M",fwd_time="-PT1M")
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time="-PT1M",fwd_time="PT3M")
        """;
    final String group100K = """
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time="PT40M",fwd_time="PT50M")
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time="PT30M",fwd_time="-PT10M")
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time="-PT10M",fwd_time="PT30M")
        """;
    final String group1M = """
        sum_contains = rolling_sum_time(ts_col='timestamp',cols=['A=num1','B=num2'],rev_time="PT4H",fwd_time="PT5H")
        min_before = rolling_min_time(ts_col='timestamp',cols=['C=num1','D=num2'],rev_time="PT3H",fwd_time="-PT1H")
        prod_after = rolling_prod_time(ts_col='timestamp',cols=['E=num1','F=num2'],rev_time="-PT1H",fwd_time="PT3H")
        """;

    @BeforeEach
    void setup() {
        var setup = """
        from deephaven.updateby import rolling_sum_time, rolling_min_time, rolling_prod_time
        from deephaven.updateby import rolling_avg_tick, rolling_max_tick, rolling_group_tick
        
        avg_contains = rolling_avg_tick(cols=['G=num1','H=num2'], rev_ticks=20, fwd_ticks=30)
        max_before = rolling_max_tick(cols=['I=num1','J=num2'], rev_ticks=20, fwd_ticks=-10)
        group_after = rolling_group_tick(cols=['K=num1','L=num2'], rev_ticks=-10, fwd_ticks=20)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    void rollingCombo0Groups6Ops() {
        setup.factors(1, 1, 1);
        runner.addSetupQuery(noGroups);
        var q = "timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after])";
        runner.test("RollingCombo- No Groups 12 Cols", q, "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo1Groups6Ops() {
        setup.factors(2, 1, 1);
        runner.addSetupQuery(group100);
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], by=['key1']);
        """;
        runner.test("RollingCombo- 1 Groups 100 Unique Vals", q, "key1", "num1", "num2", "timestamp");
    }

    @Test
    void rollingCombo2Groups6Ops() {
        setup.factors(1, 2, 1);
        runner.addSetupQuery(group10K);
        var q = """
        timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after], 
            by=['key1','key2']);
        """;
        runner.test("RollingCombo- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "num2",
                "timestamp");
    }

    @Test
    void rollingCombo3Groups6Ops() {
        setup.factors(1, 1, 1);
        runner.addSetupQuery(group100K);
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], 
            by=['key1','key2','key3']);
        """;
        runner.test("RollingCombo- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1", "num2",
                "timestamp");
    }

    @Test
    @Disabled
    void rollingCombo3Groups6OpsLarge() {
        setup.factors(1, 1, 1);
        runner.addSetupQuery(group1M);
        var q = """
        timed.update_by(ops=[sum_contains,min_before,prod_after,avg_contains,max_before,group_after], 
            by=['key1','key2','key4']);
        """;
        runner.test("RollingCombo- 3 Groups 1M Unique Combos", q, "key1", "key2", "key4", "num1", "num2",
                "timestamp");
    }

}
