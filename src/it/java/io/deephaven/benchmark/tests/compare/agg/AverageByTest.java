/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.agg;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Competitive tests for the average by group operation.
 */
@TestMethodOrder(OrderAnnotation.class)
public class AverageByTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenAverageBy() {
        runner.initDeephaven(2, "source", null, "int250", "int640", "str250");
        var setup = """
        from deephaven import agg
        from deephaven.parquet import read
        source = read('/data/source.parquet').select()
        aggs = [
            agg.avg('Avg1=int250'), agg.avg('Avg2=int640')
        ]
        """;
        var op = "source.agg_by(aggs, by=['str250', 'int640'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Average By", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowAverageBy() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        """;
        var op = "source.group_by(['str250', 'int640']).aggregate([('int250','mean'), ('int640','mean')])";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Average By", setup, op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasAverageBy() {
        runner.initPython("fastparquet", "pandas");
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        var op = """
        source.groupby(['str250', 'int640']).agg(
            Avg1=pd.NamedAgg('int250', "mean"), Avg2=pd.NamedAgg('int640', 'mean')
        )
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Average By", setup, op, msize, rsize);
    }
    
//    @Test
//    @Order(4)
//    public void flinkAverageBy() {
//        runner.usePip("apache-flink");
//        var setup = """
//        import pandas as pd
//        from pyflink.common import Row
//        from pyflink.table import (EnvironmentSettings, TableEnvironment, TableDescriptor, Schema, DataTypes, FormatDescriptor)
//        from pyflink.table.expressions import lit, col
//        from pyflink.table.udf import udtf
//
//        t_env = TableEnvironment.create(EnvironmentSettings.in_batch_mode())
//        t_env.get_config().set("parallelism.default", "1")
//
//        source = pd.read_parquet('/data/source.parquet')
//        source = t_env.from_pandas(source)
//        """;
//        var op = """
//        None
//        """;
//        var msize = "len(source)";
//        var rsize = "len(result)";
//        runner.test("Pandas Average By", setup, op, msize, rsize);
//    }

}
