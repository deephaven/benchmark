/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the firstBy table operation. Returns the first row for each group.
 */
public class FirstByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("source");
    }

//    @Test
//    void firstBy1Group() {
//        runner.setScaleFactors(20, 12);
//        var q = "source.first_by(by=['key1'])";
//        runner.test("FirstBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
//    }

    @Test
    void firstBy2Group() {
        runner.setScaleFactors(6, 1);
        var q = "source.first_by(by=['key1','key2'])";
        runner.test("FirstBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

//    @Test
//    void firstBy3Groups() {
//        runner.setScaleFactors(3, 1);
//        var q = "source.first_by(by=['key1', 'key2', 'key3'])";
//        runner.test("FirstBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
//    }

}
