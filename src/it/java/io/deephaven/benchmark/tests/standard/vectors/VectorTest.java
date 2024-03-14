/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.vectors;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for assessing the difference between sparse and dense column data for vector aggregations
 */
public class VectorTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @Test
    void vectorAggDenseData() {
        runner.setRowFactor(4);
        runner.table("source", "linearConv");
        runner.setScaleFactors(12, 12);
        runner.addSetupQuery("source = source.group_by(['key5'])");
        var q = "source.update(['Calc=avg(num2)+max(num2)-min(num2)+std(num2)-median(num2)'])";
        runner.test("Vector- 5 Calcs 1M Groups Dense Data", q, "key5", "num2");
    }
    
    @Test
    void vectorAggSparseData() {
        runner.setRowFactor(4);
        runner.table("source", "ascending");
        runner.setScaleFactors(2, 2);
        runner.addSetupQuery("source = source.group_by(['key5'])");
        var q = "source.update(['Calc=avg(num2)+max(num2)-min(num2)+std(num2)-median(num2)'])";
        runner.test("Vector- 5 Calcs 1M Groups Sparse Data", q, "key5", "num2");
    }

}
