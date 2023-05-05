/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        
        setupStr = """
        from deephaven.updateby import rolling_sum_time, rolling_min_time, rolling_prod_time
        from deephaven.updateby import rolling_avg_tick, rolling_max_tick, rolling_group_tick
         
        sum_contains = rolling_sum_time(ts_col="timestamp", cols=["U=${calc.col}"], rev_time="00:00:01", fwd_time="00:00:01")
        min_before = rolling_min_time(ts_col="timestamp", cols=["V=${calc.col}"], rev_time="00:00:03", fwd_time=int(-1e9))
        prod_after = rolling_prod_time(ts_col="timestamp", cols=["W=${calc.col}"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        avg_contains = rolling_avg_tick(cols=["X = ${calc.col}"], rev_ticks=1, fwd_ticks=1)
        max_before = rolling_max_tick(cols=["Y = ${calc.col}"], rev_ticks=3, fwd_ticks=-1)
        group_after = rolling_group_tick(cols=["Z = ${calc.col}"], rev_ticks=-1, fwd_ticks=3)
        """;
    }

    @Test
    public void rollingComboNoGroups6Ops() {
        runner.addSetupQuery(operations("int5"));
        var q = "timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after])";
        runner.test("RollingCombo- 6 Ops No Groups", q, "int5", "timestamp");
    }

    @Test
    public void rollingCombo2Groups6OpsInt() {
        runner.addSetupQuery(operations("int5"));
        var q = """
        timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after], 
            by=['str100','str150']);
        """;
        runner.test("RollingCombo- 6 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150", "int5",
                "timestamp");
    }

    @Test
    public void rollingCombo2Groups6OpsFloat() {
        runner.addSetupQuery(operations("float5"));
        var q = """
        timed.update_by(ops=[sum_contains, min_before, prod_after, avg_contains, max_before, group_after], 
            by=['str100','str150']);
        """;
        runner.test("RollingCombo- 6 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150", "float5",
                "timestamp");
    }
    
    private String operations(String type) {
        return setupStr.replace("${calc.col}", type);
    }

}
