/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.sort;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;
import io.deephaven.benchmark.tests.compare.Setup;

/**
 * Product comparison tests for sort operations. Tests read the same parquet data. To avoid an unfair
 * advantage where some products may partition or group data during the read, parquet read time is included in the
 * benchmark results.
 * <p/>
 * Each test sorts a table by a string and an integer
 */
@TestMethodOrder(OrderAnnotation.class)
public class SortTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenSort() {
        runner.initDeephaven(1, "source", null, "int640", "str250");
        var setup = "from deephaven.parquet import read";
        var op = """
        source = read('/data/source.parquet').select()
        result = source.sort(order_by=['str250', 'int640'])
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Sort", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowSort() {
        runner.initPython("pyarrow");
        var setup = "import pyarrow.dataset as ds";
        var op = """
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        result = source.sort_by([('str250','ascending'), ('int640','ascending')])
        """;
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Sort", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasSort() {
        runner.initPython("fastparquet", "pandas");
        var setup = "import pandas as pd";
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        result = source.sort_values(by=['str250','int640'])
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Sort", setup, op, msize, rsize);
    }
    
    @Test
    @Order(4)
    public void flinkSort() {
        runner.initPython("apache-flink", "jdk-11");
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        loaded_size = len(source)
        source = t_env.from_pandas(source)
        result = source.order_by(col('str250'), col('int640')).execute()
        """;
        var msize = "loaded_size";
        var rsize = "loaded_size";    // count_rows takes for ever, so here we are
        runner.test("Flink Sort", Setup.flink, op, msize, rsize);
    }

}
