/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the medianBy table operation
 */
public class MedianByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(2);
        runner.tables("source");
    }

    @Test
    public void medianBy0Groups3Cols() {
        runner.setScaleFactors(2, 2);
        var q = "source.median_by()";
        runner.test("MedianBy- No Groups 3 Cols", 1, q, "str250", "str640", "int250");
    }

    @Test
    public void medianBy1Group2Cols() {
        runner.setScaleFactors(10, 8);
        var q = "source.median_by(by=['str250'])";
        runner.test("MedianBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void medianBy1Group2ColsLarge() {
        var q = "source.median_by(by=['str1M'])";
        runner.test("MedianBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void medianBy2GroupInt() {
        runner.setScaleFactors(2, 2);
        var q = "source.median_by(by=['str250', 'str640'])";
        runner.test("MedianBy- 2 Group 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void medianBy2GroupFloat() {
        runner.setScaleFactors(2, 2);
        var q = "source.median_by(by=['str250', 'str640'])";
        runner.test("MedianBy- 2 Group 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
