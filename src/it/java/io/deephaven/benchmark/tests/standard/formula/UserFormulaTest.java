/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void udfIntArrayToIntNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(arr):
            return arr[0]
        source = source.update(['int250 = repeat(int250,5)'])
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=(int)f(int250)'])";
        runner.test("UDF- Int Array to Int No Hints", q, "int250");
    }

    @Test
    public void udfIntToIntArrayNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num):
            return jpy.array('int', [num] * 5)
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=(int[])f(int250)'])";
        runner.test("UDF- Int to Int Array No Hints", q, "int250");
    }

    @Test
    public void udf2IntsToIntNoHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num1, num2):
            return num1 + num2
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=(int)f(int250, int640)'])";
        runner.test("UDF- 2 Ints to Int No Hints", q, "int250", "int640");
    }


    @Test
    public void udfIntArrayToIntNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(arr: npt.NDArray[np.int32]) -> np.int32:
            return arr[0]
        source = source.update(['int250=repeat(int250,5)'])
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=f(int250)'])";
        runner.test("UDF- Int Array to Int Numpy Hints", q, "int250");
    }

    @Test
    public void udfIntToIntArrayNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num: np.int32) -> npt.NDArray[np.int32]:
            return np.repeat(num,5)
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=f(int250)'])";
        runner.test("UDF- Int to Int Array Numpy Hints", q, "int250");
    }

    @Test
    public void udf2IntsToIntNumpyHints() {
        runner.setScaleFactors(1, 1);
        var setup = """
        def f(num1: np.int32, num2: np.int32) -> np.int32:
            return num1 + num2
        """;
        runner.addSetupQuery(setup);
        var q = "source.select(['int250=f(int250, int640)'])";
        runner.test("UDF- 2 Ints to Int Numpy Hints", q, "int250", "int640");
    }

}
