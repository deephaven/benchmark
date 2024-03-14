/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.formula;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for running user-defined functions. These tests are meant to be compared, and so use the same data.
 * <p/>
 * Note: When scaling row count, vector size should not get bigger. That would cause more than one axis change and
 * invalidate any expected comparisons.
 * <p/>
 * Note: The "NoHints" tests do have return hints to make them equivalent to the hints tests, otherwise the return value
 * would always be a PyObject and not really the same test.
 */
public class UserFormulaTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source");
    }

    @Test
    void udfDoubleArrayToDoubleNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(arr):
            return arr[0]
        source = source.update(['num1 = repeat(num1,5)'])
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=(double)f(num1)'])";
        runner.test("UDF- Double Array to Double No Hints", q, "num1");
    }

    @Test
    void udfDoubleToDoubleArrayNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num):
            return jpy.array('double', [num] * 5)
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=(double[])f(num1)'])";
        runner.test("UDF- Double to Double Array No Hints", q, "num1");
    }

    @Test
    void udf2DoublesToDoubleNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num1, num2):
            return num1 + num2
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=(double)f(num1, num2)'])";
        runner.test("UDF- 2 Doubles to Double No Hints", q, "num1", "num2");
    }


    @Test
    void udfDoubleArrayToDoubleNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(arr: npt.NDArray[np.float64]) -> np.float64:
            return arr[0]
        source = source.update(['num1=repeat(num1,5)'])
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=f(num1)'])";
        runner.test("UDF- Double Array to Double Numpy Hints", q, "num1");
    }

    @Test
    void udfDoubleToDoubleArrayNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num: np.float64) -> npt.NDArray[np.float64]:
            return np.repeat(num,5)
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=f(num1)'])";
        runner.test("UDF- Double to Double Array Numpy Hints", q, "num1");
    }

    @Test
    void udf2DoublesToDoubleNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num1: np.float64, num2: np.float64) -> np.float64:
            return num1 + num2
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['num1=f(num1, num2)'])";
        runner.test("UDF- 2 Ints to Int Numpy Hints", q, "num1", "num2");
    }

}
