/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the tailBy table operation. Returns the last n rows for each group.
 */
public class TailByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void tailBy1Group2Cols() {
        runner.setScaleFactors(8, 2);
        var q = "source.tail_by(2, by=['str250'])";
        runner.test("TailBy- 1 Group 250 Unique Vals 2 Rows Per", 250 * 2, q, "str250", "int250");
    }

    @Test
    public void tailBy1Group2ColsLarge() {
        var q = "source.tail_by(2, by=['str1M'])";
        runner.test("TailBy- 1 Group 1M Unique Vals 2 Rows Per", 1000000 * 2, q, "str1M", "int1M");
    }

    @Test
    public void tailBy2Groups3Cols() {
        var q = "source.tail_by(2, by=['str250', 'str640'])";
        runner.test("TailBy- 2 Groups 160K Unique Combos 2 Rows Per", 160000 * 2, q, "str250", "str640", "int250");
    }

}
