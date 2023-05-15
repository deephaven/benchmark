/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
    public void setup() {
        runner.setRowFactor(4);
        runner.tables("timed");

        setupStr = """
        from deephaven.updateby import rolling_avg_time, rolling_max_tick, rolling_prod_time
        from deephaven.updateby import ema_tick, cum_min, cum_sum
         
        avg_contains = rolling_avg_time(ts_col="timestamp", cols=["U=${calc.col}"], rev_time="00:00:01", fwd_time="00:00:01")
        max_before = rolling_max_tick(cols=["V = ${calc.col}"], rev_ticks=3, fwd_ticks=-1)
        prod_after = rolling_prod_time(ts_col="timestamp", cols=["W=${calc.col}"], rev_time="-00:00:01", fwd_time=int(3e9))
        
        ema_tick_op = ema_tick(time_scale_ticks=100,cols=['X=${calc.col}'])
        min_op = cum_min(cols=['Y=${calc.col}'])
        sum_op = cum_sum(cols=['Z=${calc.col}'])
        """;
    }

    @Test
    public void mixedComboNoGroups6Ops() {
        runner.addSetupQuery(operations("int5"));
        var q = "timed.update_by(ops=[avg_contains, max_before, prod_after, ema_tick_op, min_op, sum_op])";
        runner.test("MixedCombo- 6 Ops No Groups", q, "int5", "timestamp");
    }

    @Test
    public void rollingCombo2Groups6OpsInt() {
        runner.addSetupQuery(operations("int5"));
        var q = """
        timed.update_by(ops=[avg_contains, max_before, prod_after, ema_tick_op, min_op, sum_op], 
            by=['str100','str150'])
        """;
        runner.test("MixedCombo- 6 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150",
                "int5", "timestamp");
    }

    @Test
    public void rollingCombo2Groups6OpsFloat() {
        runner.addSetupQuery(operations("float5"));
        var q = """
        timed.update_by(ops=[avg_contains, max_before, prod_after, ema_tick_op, min_op, sum_op], 
            by=['str100','str150'])
        """;
        runner.test("MixedCombo- 6 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150",
                "float5", "timestamp");
    }

    private String operations(String type) {
        return setupStr.replace("${calc.col}", type);
    }

}
