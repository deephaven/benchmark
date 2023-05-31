/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Combines multiple rolling operations
 */
public class CumComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    String setupStr = null;

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("timed");
        
        setupStr = """
        from deephaven.updateby import ema_tick, ema_time
        from deephaven.updateby import cum_max, cum_min, cum_sum, cum_prod
        
        ema_tick_op = ema_tick(decay_ticks=100,cols=['U=${calc.col}'])
        ema_time_op = ema_time(ts_col='timestamp', decay_time='00:00:02', cols=['V=${calc.col}'])
        max_op = cum_max(cols=['W=${calc.col}'])
        min_op = cum_min(cols=['X=${calc.col}'])
        sum_op = cum_sum(cols=['Y=${calc.col}'])
        prod_op = cum_prod(cols=['Z=${calc.col}'])
        """;
    }

    @Test
    public void cumComboNoGroups6Ops() {
        runner.addSetupQuery(operations("int5"));
        var q = "timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op])";
        runner.test("CumCombo- 6 Ops No Groups", q, "int5", "timestamp");
    }

    @Test
    public void cumCombo2Groups6OpsInt() {
        runner.addSetupQuery(operations("int5"));
        var q = """
        timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op], by=['str100','str150'])
        """;
        runner.test("CumCombo- 6 Ops 2 Groups 15K Unique Combos Int", q, "str100", "str150",
                "int5", "timestamp");
    }
    
    @Test
    public void cumCombo2Groups6OpsFloat() {
        runner.addSetupQuery(operations("float5"));
        var q = """
        timed.update_by(ops=[ema_tick_op, ema_time_op, max_op, min_op, sum_op, prod_op], by=['str100','str150'])
        """;
        runner.test("CumCombo- 6 Ops 2 Groups 15K Unique Combos Float", q, "str100", "str150",
                "float5", "timestamp");
    }
    
    private String operations(String type) {
        return setupStr.replace("${calc.col}", type);
    }

}
