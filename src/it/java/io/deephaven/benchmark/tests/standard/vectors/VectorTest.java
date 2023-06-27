/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.vectors;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for assessing the difference between sparse and dense column data for vector aggregations
 */
public class VectorTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @Test
    public void vectorAggDenseData() {
        runner.setRowFactor(6);
        runner.table("source", "linearConv");
        runner.setScaleFactors(12, 12);
        runner.addSetupQuery("source = source.group_by(['str1M'])");
        var q = "source.update(['Calc=avg(int1M)+max(int1M)-min(int1M)+std(int1M)-median(int1M)'])";
        runner.test("Vector- 5 Calcs 1M Groups Dense Data", q, "str1M", "int1M");
    }
    
    @Test
    public void vectorAggSparseData() {
        runner.setRowFactor(4);
        runner.table("source", "incremental");
        runner.setScaleFactors(2, 2);
        runner.addSetupQuery("source = source.group_by(['str1M'])");
        var q = "source.update(['Calc=avg(int1M)+max(int1M)-min(int1M)+std(int1M)-median(int1M)'])";
        runner.test("Vector- 5 Calcs 1M Groups Sparse Data", q, "str1M", "int1M");
    }

}
