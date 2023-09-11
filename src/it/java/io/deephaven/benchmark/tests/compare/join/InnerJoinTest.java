/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.join;

import static org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;
import io.deephaven.benchmark.tests.compare.Setup;

/**
 * Product comparison tests for inner join operations. Tests read the same parquet data. To avoid an unfair advantage
 * where some products may partition or group data during the read, parquet read time is included in the benchmark
 * results.
 * <p/>
 * Each test produces a table that is the result of two tables intersected by a string and an integer.
 * <p/>
 * Data generation only happens in the first tests, the Deephaven test. Tests can be run individually, but only after
 * the desired data has been generated.
 */
@TestMethodOrder(OrderAnnotation.class)
public class InnerJoinTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenInnerJoin() {
        runner.initDeephaven(2, "source", "right", "int1M", "str250", "r_int1M", "r_str250");
        var setup = "from deephaven.parquet import read";
        var op = """
        source = read('/data/source.parquet').select()
        right = read('/data/right.parquet').select()
        result = source.join(right, on=['str250 = r_str250', 'int1M = r_int1M'])
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Inner Join", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowInnerJoin() {
        runner.initPython("pyarrow");
        var setup = "import pyarrow.dataset as ds";
        var op = """
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        right = ds.dataset('/data/right.parquet', format="parquet").to_table()
        result = source.join(right, keys=['str250','int1M'], right_keys=['r_str250','r_int1M'], join_type='inner')    
        """;
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Inner Join", setup, op, msize, rsize);
    }

    @Test
    @Order(3)
    public void pandasInnerJoin() {
        runner.initPython("fastparquet", "pandas");
        var setup = "import pandas as pd";
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        right = pd.read_parquet('/data/right.parquet')
        result = source.merge(right, left_on=['str250','int1M'], right_on=['r_str250','r_int1M'], how='inner')
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Inner Join", setup, op, msize, rsize);
    }

    @Test
    @Order(4)
    @Disabled
    public void flinkInnerJoin() {
        runner.initPython("apache-flink", "jdk-11");
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        loaded_size = len(source)
        source = t_env.from_pandas(source)
        right = pd.read_parquet('/data/right.parquet')
        right = t_env.from_pandas(right)
        result = source.join(right, (col('str250') == col('r_str250')) & (col('int1M') == col('r_int1M'))).to_pandas()
        """;
        var msize = "loaded_size";
        var rsize = "len(result)";
        runner.test("Flink Inner Join", Setup.flink(runner), op, msize, rsize);
    }

}
