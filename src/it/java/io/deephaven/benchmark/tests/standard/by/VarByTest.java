/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the varBy table operation. Returns the variance for each group.
 */
public class VarByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    void varBy0Group() {
        runner.setScaleFactors(30, 30);
        var q = "source.var_by()";
        runner.test("VarBy- No Groups", 1, q, "key3", "num1", "num2");
    }

    @Test
    void varBy1Group() {
        runner.setScaleFactors(11, 10);
        var q = "source.var_by(by=['key1'])";
        runner.test("VarBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void varBy2Groups() {
        runner.setScaleFactors(3, 2);
        var q = "source.var_by(by=['key1', 'key2'])";
        runner.test("VarBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void varBy3Groups() {
        runner.setScaleFactors(2, 1);
        var q = "source.var_by(by=['key1', 'key2', 'key3'])";
        runner.test("VarBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
