/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Calculates a cumulative product for specified columns and places
 * the result into a new column for each row.
 */
public class CumProdTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.api().table("source").random()
                .add("int5", "int", "[1-5]")
                .add("int10", "int", "[1-10]")
                .add("str100", "string", "s[1-100]")
                .add("str150", "string", "[1-150]s")
                .generateParquet();
        runner.api().query("from deephaven.updateby import cum_prod").execute();
    }

    @Test
    public void cumProd1Group2Cols() {
        var q = "source.update_by(ops=cum_prod(cols=['X1=int5']), by=['str100'])";
        runner.test("CumProd- 1 Group 100 Unique Vals 2 Col", runner.scaleRowCount, q, "str100", "int5");
    }

    @Test
    public void cumProd1Group3Cols() {
        var q = "source.update_by(ops=cum_prod(cols=['X=int5','CP2=int10']), by=['str100'])";
        runner.test("CumProd- 1 Group 100 Unique Vals 3 Cols", runner.scaleRowCount, q, "str100", "int5", "int10");
    }

    @Test
    public void cumProd2Groups3Cols() {
        var q = "source.update_by(ops=cum_prod(cols=['X=int5']), by=['str100','str150'])";
        runner.test("CumProd- 2 Groups 160K Unique Combos 3 Cols", runner.scaleRowCount, q, "str100", "str150", "int5");
    }

}
