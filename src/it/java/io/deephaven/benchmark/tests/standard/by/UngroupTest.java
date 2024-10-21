/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the groupBy table operation. ungroups column content. It is the inverse of the group_by method.
 * Ungroup unwraps columns containing either Deephaven arrays or java arrays.
 * <p>
 * Note: These test do group then ungroup, since the data generator does not support arrays
 */
public class UngroupTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void ungroup1Group2Cols() {
        runner.setScaleFactors(15, 3);
        var q = "source.group_by(by=['str250']).ungroup(cols=['int250'])";
        runner.test("Ungroup- 1 Group 250 Unique Vals", q, "str250", "int250");
    }

    @Test
    public void ungroup1Group2ColsLarge() {
        var q = "source.group_by(by=['str1M']).ungroup(cols=['int1M'])";
        runner.test("Ungroup- 1 Group 1M Unique Vals", q, "str1M", "int1M");
    }

    @Test
    public void ungroup2Group3Cols() {
        runner.setScaleFactors(2, 1);
        var q = "source.group_by(by=['str250', 'str640']).ungroup(cols=['int250'])";
        runner.test("Ungroup- 2 Group 160K Unique Combos", q, "str250", "str640", "int250");
    }

}
