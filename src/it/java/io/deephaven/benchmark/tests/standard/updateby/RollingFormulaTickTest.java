/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a tick-based rolling formula. The result table contains
 * additional columns with windowed rolling formulas for each specified column in the source table.
 * <p>
 * Note: This test must contain benchmarks and <code>rev_ticks/fwd_ticks</code> that are comparable to
 * <code>RollingFormulaTimeTest</code>
 */
public class RollingFormulaTickTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("timed");
        var s = """
        from deephaven.updateby import rolling_formula_tick
        """;
        runner.addSetupQuery(s);
    }

    @Test
    void rollingFormulaParamTick2Groups3Ops() {
        runner.setScaleFactors(5.2, 2);
        runner.addSetupQuery("""
        contains = rolling_formula_tick(formula="avg(x)", formula_param="x", cols=["Contains=num1"], 
            rev_ticks=20, fwd_ticks=30)
        """);
        var q = "timed.update_by(ops=[contains], by=['key1','key2'])";
        runner.test("RollingFormulaParamTick- 2 Groups 100K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void rollingFormulaGeneralTick0Group3Ops() {
        runner.setScaleFactors(0.15, 0.15);
        runner.addSetupQuery("""
        contains = rolling_formula_tick(formula="Contains=avg(num1)", rev_ticks=2000, fwd_ticks=3000)
        """);
        var q = "timed.update_by(ops=[contains])";
        runner.test("RollingFormulaGeneralTick- No Groups 1 Col", q, "num1");
    }

    @Test
    void rollingFormulaGeneralTick1Group3Ops() {
        runner.setScaleFactors(17, 4.3);
        runner.addSetupQuery("""
        contains = rolling_formula_tick(formula="Contains=avg(num1)", rev_ticks=20, fwd_ticks=30)
        """);
        var q = "timed.update_by(ops=[contains], by=['key1'])";
        runner.test("RollingFormulaGeneralTick- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void rollingFormulaGeneralTick2Groups3Ops() {
        runner.setScaleFactors(5.2, 2);
        runner.addSetupQuery("""
        contains = rolling_formula_tick(formula="Contains=avg(num1)", rev_ticks=20, fwd_ticks=30)
        """);
        var q = "timed.update_by(ops=[contains], by=['key1','key2'])";
        runner.test("RollingFormulaGeneralTick- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

}
