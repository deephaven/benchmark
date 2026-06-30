/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.train;

import org.junit.jupiter.api.*;

/**
 * Training tests for the formula table operations (e.g. udf, inline). See <code>TrainTestRunner</code> for more
 * information.
 */
public class FormulaTrainTest {
    final TrainTestRunner runner = new TrainTestRunner(this);

    void setup(double staticRowFactor, double incRowFactor) {
        runner.tables(staticRowFactor, incRowFactor, "timed");
    }

    @Test
    void formulaUdf() {
        setup(9, 9);
        var setup = """
        def f_py(num1: float, num2: float) -> float:
            return (num2 + num1) / 2
        def f_np(num1: np.float64, num2: np.float64) -> np.float64:
            return num1 + num2
        """;
        runner.addSetupQuery(setup);
        var q = "timed.view(['New1 = f_py(num1, num2)','New2 = f_np(num1, num2)']).sum_by()";
        runner.test("Formula- UDF 2 Calcs", 1, q, "num1", "num2");
    }

    @Test
    void formulaInline() {
        setup(467, 467);
        var q = "timed.view(['New1 = (float)((num2 + num1) / 2)', 'New2 = (float)(num1 + num2)']).sum_by()";
        runner.test("Formula- Inline 2 Calcs", 1, q, "num1", "num2");
    }
    
    @Test
    @Disabled
    void formulaDate() {
        setup(3, 3);
        var q = """
        timed.view([
            'New1 = parseDuration(`PT4H52M14S`).toHours()', 
            'New1 = parseInstant(`2023-05-31T04:52:14.001 ET`).getEpochSecond()'
        ]).sum_by()
        """;
        runner.test("Formula- Inline 2 Dates", 1, q, "num1", "num2");
    }

}
