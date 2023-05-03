/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the headBy table operation. Returns the first n rows for each group.
 */
public class HeadByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void headBy1Group2Cols() {
        runner.setScaleFactors(8, 2);
        var q = "source.head_by(2, by=['str250'])";
        runner.test("HeadBy- 1 Group 250 Unique Vals 2 Rows Per", 250 * 2, q, "str250", "int250");
    }

    @Test
    public void headBy1Group2ColsLarge() {
        var q = "source.head_by(2, by=['str1M'])";
        runner.test("HeadBy- 1 Group 1M Unique Vals 2 Rows Per", 1000000 * 2, q, "str1M", "int1M");
    }

    @Test
    public void headBy2Group3Cols() {
        var q = "source.head_by(2, by=['str250', 'str640'])";
        runner.test("LastBy- 2 Group 160K Unique Combos 2 Rows Per", 160000 * 2, q, "str250", "str640", "int250");
    }

}
