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
        runner.initDeephaven(1, "source", null, "int640", "str250");
        var setup = """
        from deephaven.parquet import read
        source = read('/data/source.parquet').select()
        """;
        var op = "source.sort(order_by=['str250', 'int640'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Sort", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowSort() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        """;
        var op = "source.sort_by([('str250','ascending'), ('int640','ascending')])";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Sort", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasSort() {
        runner.initPython("fastparquet", "pandas");
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        var op = "source.sort_values(by=['str250','int640'])";
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Sort", setup, op, msize, rsize);
    }

}
