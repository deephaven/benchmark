/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.join;

import static org.junit.jupiter.api.MethodOrderer.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the inner join operation.
 */
@TestMethodOrder(OrderAnnotation.class)
public class JoinTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenJoin() {
        runner.tables("source", "right");
        var setup = """
        source = read('/data/source.parquet').select()
        right = read('/data/right.parquet').select()
        """;
        runner.addSetupQuery(setup);

        var op = "source.join(right, on=['str250=r_str250', 'str1M=r_str1M'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Join", op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowJoin() {
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        right = ds.dataset('/data/right.parquet', format="parquet").to_table()
        """;
        runner.addSetupQuery(setup);

        var op = "source.join(right, keys=['str250','str1M'], right_keys=['r_str250','r_str1M'], join_type='inner')";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Join", op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasJoin() {
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        right = pd.read_parquet('/data/right.parquet')
        """;
        runner.addSetupQuery(setup);

        var op = "source.merge(right, left_on=['str250','str1M'], right_on=['r_str250','r_str1M'], how='inner')";
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Join", op, msize, rsize);
    }

}
