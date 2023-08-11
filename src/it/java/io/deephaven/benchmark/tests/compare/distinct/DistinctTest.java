/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.distinct;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the distinct operation
 */
@TestMethodOrder(OrderAnnotation.class)
public class DistinctTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenDistinct() {
        runner.initDeephaven(2, "source", null, "int640", "str250");
        var setup = """
        from deephaven.parquet import read
        source = read('/data/source.parquet').select()
        """;
        var op = "source.select_distinct(formulas=['str250', 'int640'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Distinct", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowDistinct() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        """;
        var op = "source.group_by(['str250', 'int640']).aggregate([])";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Distinct", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasDistinct() {
        runner.initPython("fastparquet", "pandas");
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        var op = "source.drop_duplicates(subset=['str250','int640'], keep='last')";
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Distinct", setup, op, msize, rsize);
    }

}
