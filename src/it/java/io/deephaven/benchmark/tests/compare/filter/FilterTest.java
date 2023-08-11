/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.filter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the filter (where) operation.   160,000,000 scale
 */
@TestMethodOrder(OrderAnnotation.class)
public class FilterTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenFilter() {
        runner.initDeephaven(2, "source", null, "str250", "int640");
        var setup = """
        from deephaven.parquet import read
        source = read('/data/source.parquet').select()
        """;
        var op = """
        source.where(["str250 = '250'", "int640 > 100", "int640 < 540"]);
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Filter", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowFilter() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow.dataset as ds
        import pyarrow.compute as pc
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        expr = (pc.field('str250') == '250') & (pc.field('int640') > 100) & (pc.field('int640') < 540)
        """;
        var op = "source.filter(expr)";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Filter", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasFilter() {
        runner.initPython("fastparquet", "pandas");
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        var op = """
        source.query("str250 == '250' & int640 > 100 & int640 < 540")
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Filter", setup, op, msize, rsize);
    }

}
