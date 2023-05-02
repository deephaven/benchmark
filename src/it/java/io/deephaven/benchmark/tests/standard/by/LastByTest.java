/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the lastBy table operation. Returns the last row for each group.
 */
public class LastByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void lastBy1Group2Cols() {
        runner.setScaleFactors(10, 6);
        var q = "source.last_by(by=['str250'])";
        runner.test("LastBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void lastBy1Group2ColsLarge() {
        var q = "source.last_by(by=['str1M'])";
        runner.test("LastBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int250");
    }

    @Test
    public void lastBy2Groups3Cols() {
        var q = "source.last_by(by=['str250', 'str640'])";
        runner.test("LastBy- 2 Group 160K Unique Combos", 160000, q, "str250", "str640", "int250");
    }

}
