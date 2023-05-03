/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the countBy table operation. Returns the number of rows for each group.
 */
public class CountByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(6);
        runner.tables("source");
    }

    @Test
    public void countBy1IntGroup1Col() {
        runner.setScaleFactors(20, 20);
        var q = "source.count_by('count', by=['int250'])";
        runner.test("CountBy- 1 Int Group 250 Unique Vals", 250, q, "int250");
    }

    @Test
    public void countBy1IntGroup1ColLarge() {
        runner.setScaleFactors(3, 2);
        var q = "source.count_by('count', by=['int1M'])";
        runner.test("CountBy- 1 Int Group 1M Unique Vals", 1000000, q, "int1M");
    }

    @Test
    public void countBy1StringGroup1Col() {
        runner.setScaleFactors(15, 15);
        var q = "source.count_by('count', by=['str250'])";
        runner.test("CountBy- 1 String Group 250 Unique Vals", 250, q, "str250");
    }

    @Test
    public void countBy2IntGroups2Cols() {
        runner.setScaleFactors(5, 5);
        var q = "source.count_by('count', by=['int250', 'int640'])";
        runner.test("CountBy- 2 Int Groups 160K Unique Combos", 160000, q, "int250", "int640");
    }

    @Test
    public void countBy2StringGroups2Cols() {
        runner.setScaleFactors(5, 5);
        var q = "source.count_by('count', by=['str250', 'str640'])";
        runner.test("CountBy- 2 String Groups 160K Unique Combos", 160000, q, "str250", "str640");
    }

}
