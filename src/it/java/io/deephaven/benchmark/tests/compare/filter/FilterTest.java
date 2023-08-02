/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.filter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the filter (where) operation.
 */
@TestMethodOrder(OrderAnnotation.class)
public class FilterTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenFilter() {
        runner.tables("source");
        var setup = "source = read('/data/source.parquet').select()";
        runner.addSetupQuery(setup);

        var op = """
        source.where(["str250 = '250'", "str640 = '640'"]);
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Filter", op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowFilter() {
        var setup = """
        import pyarrow.dataset as ds
        import pyarrow.compute as pc
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        expr = (pc.field('str250') == '250') & (pc.field('str640') == '640')
        """;
        runner.addSetupQuery(setup);

        var op = "source.filter(expr)";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Filter", op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasFilter() {
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        runner.addSetupQuery(setup);

        var op = """
        source.query("str250 == '250' & str640 == '640'")
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Filter", op, msize, rsize);
    }

}
