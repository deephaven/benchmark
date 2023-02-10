/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. Groups column content into arrays.
 */
public class GroupByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void groupBy0Groups3Cols() {
        var q = "source.group_by()";
        runner.test("GroupBy- No Groups 3 Cols", 1, q, "str250", "str640", "int250");
    }

    @Test
    public void groupBy1Group2Cols() {
        var q = "source.group_by(by=['str250'])";
        runner.test("GroupBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void groupBy1Group2ColsLarge() {
        var q = "source.group_by(by=['str1M'])";
        runner.test("GroupBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int1M");
    }

    @Test
    public void groupBy2Group3Cols() {
        var q = "source.group_by(by=['str250', 'str640'])";
        runner.test("GroupBy- 2 Group 160K Unique Combos", 160000, q, "str250", "str640", "int250");
    }

}
