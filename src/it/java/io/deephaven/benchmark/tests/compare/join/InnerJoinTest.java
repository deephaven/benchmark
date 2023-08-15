/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.join;

import static org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the inner join operation.
 */
@TestMethodOrder(OrderAnnotation.class)
public class InnerJoinTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenJoin() {
        runner.initDeephaven(2, "source", "right", "int1M", "str250", "r_int1M", "r_str250");
        var setup = """
        from deephaven.parquet import read
        source = read('/data/source.parquet').select()
        right = read('/data/right.parquet').select()
        """;
        var op = "source.join(right, on=['str250 = r_str250', 'int1M = r_int1M'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Inner Join", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowJoin() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        right = ds.dataset('/data/right.parquet', format="parquet").to_table()
        """;
        var op = "source.join(right, keys=['str250','int1M'], right_keys=['r_str250','r_int1M'], join_type='inner')";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Inner Join", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasJoin() {
        runner.initPython("fastparquet", "pandas");
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        right = pd.read_parquet('/data/right.parquet')
        """;
        var op = "source.merge(right, left_on=['str250','int1M'], right_on=['r_str250','r_int1M'], how='inner')";
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Inner Join", setup, op, msize, rsize);
    }

}
