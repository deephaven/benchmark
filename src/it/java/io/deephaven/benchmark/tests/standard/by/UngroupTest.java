/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
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
    public void setup() {
        runner.tables("source");
    }
    
    @Test
    public void ungroup0Groups() {
        runner.setScaleFactors(15, 3);
        var q = "source.group_by().ungroup()";
        runner.test("Ungroup- No Groups", q, "key1", "key2", "num1", "num2");
    }

    @Test
    public void ungroup1Group() {
        runner.setScaleFactors(15, 3);
        var q = "source.group_by(by=['key1']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 1 Group 100 Unique Vals", q, "key1", "num1");
    }

    @Test
    public void ungroup2Groups() {
        var q = "source.group_by(by=['key1','key2']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1");
    }

    @Test
    public void ungroup3Groups() {
        runner.setScaleFactors(2, 1);
        var q = "source.group_by(by=['key1', 'key2', 'key3']).ungroup(cols=['num1'])";
        runner.test("Ungroup- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1");
    }

}
