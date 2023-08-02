/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.sort;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the sort operation. Sorts rows of data from the source according to the defined columns
 */
@TestMethodOrder(OrderAnnotation.class)
public class SortTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenSort() {
        runner.tables("source");
        var setup = "source = read('/data/source.parquet').select()";
        runner.addSetupQuery(setup);

        var op = "source.sort(order_by=['str250', 'str640'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Sort", op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowSort() {
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        """;
        runner.addSetupQuery(setup);

        var op = "source.sort_by([('str250','ascending'), ('str640','ascending')])";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Sort", op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasSort() {
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        runner.addSetupQuery(setup);

        var op = "source.sort_values(by=['str250','str640'])";
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Sort", op, msize, rsize);
    }

}
