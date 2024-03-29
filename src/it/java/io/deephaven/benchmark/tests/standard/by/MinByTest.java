/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the minBy table operation. Returns the minimum value for each group.
 */
public class MinByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
    }

    @Test
    public void minBy0Groups3Cols() {
        runner.setScaleFactors(20, 1);
        var q = "source.min_by()";
        runner.test("MinBy- No Groups 3 Cols", 1, q, "str250", "str640", "int250");
    }

    @Test
    public void minBy1Group2Cols() {
        runner.setScaleFactors(15, 4);
        var q = "source.min_by(by=['str250'])";
        runner.test("MinBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void minBy1Group2ColsLarge() {
        runner.setScaleFactors(2, 1);
        var q = "source.min_by(by=['str1M'])";
        runner.test("MinBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void minBy2GroupsInt() {
        runner.setScaleFactors(4, 1);
        var q = "source.min_by(by=['str250', 'str640'])";
        runner.test("MinBy- 2 Group 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void minBy2GroupsFloat() {
        runner.setScaleFactors(3, 1);
        var q = "source.min_by(by=['str250', 'str640'])";
        runner.test("MinBy- 2 Group 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
