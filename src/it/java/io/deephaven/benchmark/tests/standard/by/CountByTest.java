/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
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
    public void countBy1Group() {
        runner.setScaleFactors(20, 20);
        var q = "source.count_by('count', by=['key1'])";
        runner.test("CountBy- 1 Group 100 Unique Vals", 100, q, "key1");
    }

    @Test
    public void countBy2Groups() {
        runner.setScaleFactors(3, 2);
        var q = "source.count_by('count', by=['key1','key2'])";
        runner.test("CountBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2");
    }

    @Test
    public void countBy3Groups() {
        runner.setScaleFactors(15, 15);
        var q = "source.count_by('count', by=['key1','key2','key3'])";
        runner.test("CountBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3");
    }

}
