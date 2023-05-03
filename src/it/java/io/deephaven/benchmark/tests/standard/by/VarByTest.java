/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the varBy table operation. Returns the variance for each group.
 */
public class VarByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void varBy1Group2Cols() {
        runner.setScaleFactors(15, 15);
        var q = "source.var_by(by=['str250'])";
        runner.test("VarBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void varBy1Group2ColsLarge() {
        var q = "source.var_by(by=['str1M'])";
        runner.test("VarBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void varBy2GroupsInt() {
        runner.setScaleFactors(3, 2);
        var q = "source.var_by(by=['str250', 'str640'])";
        runner.test("VarBy- 2 Group 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void varBy2GroupsFloat() {
        runner.setScaleFactors(3, 2);
        var q = "source.var_by(by=['str250', 'str640'])";
        runner.test("VarBy- 2 Group 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
