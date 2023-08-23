/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.distinct;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;
import io.deephaven.benchmark.tests.compare.Setup;

/**
 * Product comparison tests for the distinct (or select distinct) group operation. Tests read the same parquet data. To
 * avoid an unfair advantage where some products may partition or group data during the read, parquet read time is
 * included in the benchmark results.
 * <p/>
 * Each test produces a table result that contains rows unique according to a string and an integer
 */
@TestMethodOrder(OrderAnnotation.class)
public class DistinctTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenDistinct() {
        runner.initDeephaven(2, "source", null, "int640", "str250");
        var setup = "from deephaven.parquet import read";
        var op = """
        source = read('/data/source.parquet').select()
        result = source.select_distinct(formulas=['str250', 'int640'])
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Distinct", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowDistinct() {
        runner.initPython("pyarrow");
        var setup = "import pyarrow.dataset as ds";
        var op = """
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        result = source.group_by(['str250', 'int640']).aggregate([])
        """;
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Distinct", setup, op, msize, rsize);
    }

    @Test
    @Order(3)
    public void pandasDistinct() {
        runner.initPython("fastparquet", "pandas");
        var setup = "import pandas as pd";
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        result = source.drop_duplicates(subset=['str250','int640'], keep='last')
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Distinct", setup, op, msize, rsize);
    }

    @Test
    @Order(4)
    public void flinkDistinct() {
        runner.initPython("apache-flink", "jdk-11");
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        loaded_size = len(source)
        source = t_env.from_pandas(source)
        result = source.select(col('str250'), col('int640')).distinct().execute()
        """;
        var msize = "loaded_size";
        var rsize = "count_rows(result)";
        runner.test("Flink Distinct", Setup.flink, op, msize, rsize);
    }

}
