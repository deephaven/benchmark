/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the avgBy table operation. Returns the average (mean) of each non-key column for each group.
 */
public class AvgByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void avgBy1Group2Cols() {
        runner.setScaleFactors(10, 10);
        var q = "source.avg_by(by=['str250'])";
        runner.test("AvgBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void avgBy1Group2ColsLarge() {
        var q = "source.avg_by(by=['str1M'])";
        runner.test("AvgBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void avgBy2GroupInt() {
        runner.setScaleFactors(3, 2);
        var q = "source.avg_by(by=['str250', 'str640'])";
        runner.test("AvgBy- 2 Groups 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void avgBy2GroupFloat() {
        runner.setScaleFactors(3, 2);
        var q = "source.avg_by(by=['str250', 'str640'])";
        runner.test("AvgBy- 2 Groups 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
