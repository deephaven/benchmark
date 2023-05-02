/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the stdBy table operation. Returns the standard deviation for each group.
 */
public class StdByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
        runner.setScaleFactors(15, 12);
    }

    @Test
    public void stdBy1Group2Cols() {
        var q = "source.std_by(by=['str250'])";
        runner.test("StdBy- 1 Group 250 Unique Vals", 250, q, "str250", "int250");
    }

    @Test
    public void stdBy1Group2ColsLarge() {
        var q = "source.std_by(by=['str1M'])";
        runner.test("StdBy- 1 Group 1M Unique Vals", 1000000, q, "str1M", "int250");
    }

    @Test
    public void stdBy2GroupsInt() {
        var q = "source.std_by(by=['str250', 'str640'])";
        runner.test("StdBy- 2 Group 160K Unique Combos Int", 160000, q, "str250", "str640", "int250");
    }
    
    @Test
    public void stdBy2GroupsFloat() {
        var q = "source.std_by(by=['str250', 'str640'])";
        runner.test("StdBy- 2 Group 160K Unique Combos Float", 160000, q, "str250", "str640", "float5");
    }

}
