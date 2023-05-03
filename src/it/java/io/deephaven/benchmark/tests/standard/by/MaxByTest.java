/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the maxBy table operation. Returns the maximum value for each group.
 */
public class MaxByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void maxBy0Groups3Cols() {
        runner.setScaleFactors(35, 1);
        var q = "source.max_by()";
        runner.test("MaxBy- No Groups 3 Cols", 1, q, "str250", "str640", "int250");
    }

    @Test
    public void maxBy1Group2Cols() {
        runner.setScaleFactors(15, 4);
        var q = "source.max_by(by=['str250'])";
        runner.test("MaxBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void maxBy1Group2ColsLarge() {
        runner.setScaleFactors(2, 1);
        var q = "source.max_by(by=['str1M'])";
        runner.test("MaxBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void maxBy2GroupsInt() {
        runner.setScaleFactors(4, 1);
        var q = "source.max_by(by=['str250', 'str640'])";
        runner.test("MaxBy- 2 Group 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void maxBy2GroupsFloat() {
        runner.setScaleFactors(4, 1);
        var q = "source.max_by(by=['str250', 'str640'])";
        runner.test("MaxBy- 2 Group 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
