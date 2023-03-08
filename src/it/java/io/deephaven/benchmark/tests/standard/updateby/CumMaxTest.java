/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative maximum for specified columns and places the
 * result into a new column for each row.
 */
public class CumMaxTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int5", "int", "[1-5]")
                .add("int10", "int", "[1-10]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        runner.addSetupQuery("from deephaven.updateby import cum_max");
    }

    @Test
    public void cumMax0Group1Col() {
        var q = "source.update_by(ops=cum_max(cols=['X=int5']))";
        runner.test("CumMax- No Groups 1 Col", runner.scaleRowCount, q, "int5");
    }

    @Test
    public void cumMax0Group2Cols() {
        var q = "source.update_by(ops=cum_max(cols=['X=int5','Y=int10']))";
        runner.test("CumMax- No Groups 2 Cols", runner.scaleRowCount, q, "int5", "int10");
    }

    @Test
    public void cumMax1Group2Cols() {
        var q = "source.update_by(ops=cum_max(cols=['X=int5']), by=['str100'])";
        runner.test("CumMax- 1 Group 100 Unique Vals 2 Cols", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void cumMax1Group3Cols() {
        var q = "source.update_by(ops=cum_max(cols=['X=int5','Y=int10']), by=['str100'])";
        runner.test("CumMax- 1 Group 100 Unique Vals 3 Cols", runner.scaleRowCount, q, "str100", "int5", "int10");
    }

    @Test
    public void cumMax2Groups3Cols() {
        var q = "source.update_by(ops=cum_max(cols=['X=int5']), by=['str100','str150'])";
        runner.test("CumMax- 2 Groups 160K Unique Combos 3 Cols", runner.scaleRowCount, q, "str100", "str150", "int5");
    }

}
