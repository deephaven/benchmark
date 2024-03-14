/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. Ungroups column content. It is the inverse of groupBy.
 * Ungroup unwraps columns containing Deephaven arrays or vectors.
 * <p/>
 * Note: These tests do group then ungroup, since the data generator does not support arrays
 */
public class UngroupTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(4);
        runner.tables("source");
    }
    
    @Test
    void ungroup1Group() {
        runner.setScaleFactors(12, 2);
        var q = "source.group_by(by=['key1']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    void ungroup2Groups() {
        runner.setScaleFactors(2, 2);
        var q = "source.group_by(by=['key1','key2']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    void ungroup3Groups() {
        runner.setScaleFactors(1, 1);
        var q = "source.group_by(by=['key1', 'key2', 'key3']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
