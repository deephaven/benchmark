/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the firstBy table operation. Returns the first row for each group.
 */
public class FirstByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void firstBy1Group2Cols() {
        runner.setScaleFactors(15, 10);
        var q = "source.first_by(by=['str250'])";
        runner.test("FirstBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void firstBy1Group2ColsLarge() {
        runner.setScaleFactors(2, 1);
        var q = "source.first_by(by=['str1M'])";
        runner.test("FirstBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int250");
    }

    @Test
    public void firstBy2Groups3Cols() {
        runner.setScaleFactors(3, 1);
        var q = "source.first_by(by=['str250', 'str640'])";
        runner.test("FirstBy- 2 Group 160K Unique Combos", 160000, q, "str250", "str640", "int250");
    }

}
