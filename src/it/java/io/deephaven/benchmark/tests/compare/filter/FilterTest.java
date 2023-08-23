/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.filter;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;
import io.deephaven.benchmark.tests.compare.Setup;

/**
 * Product comparison tests for filter (where) operations. Tests read the same parquet data. To avoid an unfair
 * advantage where some products may partition or group data during the read, parquet read time is included in the
 * benchmark results.
 * <p/>
 * Each test produces a table result filtered by three criteria; value is an exact string, value > an integer, value <
 * an integer
 */
@TestMethodOrder(OrderAnnotation.class)
public class FilterTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenFilter() {
        runner.initDeephaven(2, "source", null, "str250", "int640");
        var setup = "from deephaven.parquet import read";
        var op = """
        source = read('/data/source.parquet').select()
        result = source.where(["str250 = '250'", "int640 > 100", "int640 < 540"])
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
        """;
        var op = """
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        expr = (pc.field('str250') == '250') & (pc.field('int640') > 100) & (pc.field('int640') < 540)
        result = source.filter(expr)
        """;
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Filter", setup, op, msize, rsize);
    }

    @Test
    @Order(3)
    public void pandasFilter() {
        runner.initPython("fastparquet", "pandas");
        var setup = "import pandas as pd";
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        result = source.query("str250 == '250' & int640 > 100 & int640 < 540")
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Filter", setup, op, msize, rsize);
    }
    
    @Test
    @Order(4)
    public void flinkFilter() {
        runner.initPython("apache-flink", "jdk-11");
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        loaded_size = len(source)
        source = t_env.from_pandas(source)
        result = source.filter((col('str250') == '250') & (col('int640') > 100) & (col('int640') < 540)).execute()
        """;
        var msize = "loaded_size";
        var rsize = "count_rows(result)";
        runner.test("Flink Filter", Setup.flink, op, msize, rsize);
    }

}
