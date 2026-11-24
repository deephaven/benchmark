/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.join;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the exact_join table operation. Column values will be added to each left row from exactly one
 * matched row from the right table or null if no matches
 */
public class ExactJoinTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source", "right");
    }

    @Test
    void ExactJoinOn1Col() {
        runner.setScaleFactors(4.7, 1.2);
        var q = "source.exact_join(right, on=['key5 = r_key5'])";
        runner.test("ExactJoin- Join On 1 Col", q, "key5", "num1");
    }

    @Test
    void ExactJoinOn2Cols() {
        runner.setScaleFactors(16, 4);
        var q = "source.exact_join(right, on=['key1 = r_wild', 'key2 = r_key2'])";
        runner.test("ExactJoin- Join On 2 Cols", q, "key1", "key2", "num1");
    }
    
    @Test
    void ExactJoinOn3Cols() {
        runner.setScaleFactors(12, 3.3);
        var q = "source.exact_join(right, on=['key1 = r_wild', 'key2 = r_key2', 'key1 = r_key1'])";
        runner.test("ExactJoin- Join On 3 Cols", q, "key1", "key2", "num1");
    }

}
