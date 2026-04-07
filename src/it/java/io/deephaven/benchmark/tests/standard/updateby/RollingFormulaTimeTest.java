/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling formula. The result table contains
 * additional columns with windowed rolling formulas for each specified column in the source table.
 * <p>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingFormulaTickTest</code>
 */
public class RollingFormulaTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.setScaleFactors(staticFactor, incFactor);
        runner.tables("timed");
        var s = """
        from deephaven.updateby import rolling_formula_time
        """;
        runner.addSetupQuery(s);
    }

    @Test
    void rollingFormulaParamTime2Groups3Ops() {
        setup(2, 2, 1);
        runner.addSetupQuery("""
        contains = rolling_formula_time(formula="avg(x)", formula_param="x", ts_col="timestamp",
            cols=["Contains=num1"], rev_time="PT4M", fwd_time="PT5M")
        """);
        var q = "timed.update_by(ops=[contains], by=['key1','key2'])";
        runner.test("RollingFormulaParamTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingFormulaGeneralTime0Group3Ops() {
        setup(1, 1, 1);
        runner.addSetupQuery("""
        contains = rolling_formula_time(formula="Contains=avg(num1)", ts_col="timestamp", rev_time="PT2S", 
            fwd_time="PT3S")
        """);
        var q = "timed.update_by(ops=[contains])";
        runner.test("RollingFormulaGeneralTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void rollingFormulaGeneralTime1Group3Ops() {
        setup(3, 2, 1);
        runner.addSetupQuery("""
        contains = rolling_formula_time(formula="Contains=avg(num1)", ts_col="timestamp", rev_time="PT2S", 
            fwd_time="PT3S")
        """);
        var q = "timed.update_by(ops=[contains], by=['key1'])";
        runner.test("RollingFormulaGeneralTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingFormulaGeneralTime2Groups3Ops() {
        setup(2, 2, 1);
        runner.addSetupQuery("""
        contains = rolling_formula_time(formula="Contains=avg(num1)", ts_col="timestamp", rev_time="PT4M", 
            fwd_time="PT5M")
        """);
        var q = "timed.update_by(ops=[contains], by=['key1','key2'])";
        runner.test("RollingFormulaGeneralTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

}
