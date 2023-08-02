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
        runner.tables("source");
        var setup = """
        from deephaven import agg
        source = read('/data/source.parquet').select()
        aggs = [
            agg.avg('Avg1=int250'), agg.avg('Avg2=int640')
        ]
        """;
        runner.addSetupQuery(setup);

        var op = "source.agg_by(aggs, by=['str250', 'str640'])";
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Average By", op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowAverageBy() {
        var setup = """
        import pyarrow.dataset as ds
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        """;
        runner.addSetupQuery(setup);

        var op = "source.group_by(['str250', 'str640']).aggregate([('int250','mean'), ('int640','mean')])";
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Average By", op, msize, rsize);
    }
    
    @Test
    @Order(3)
    public void pandasAverageBy() {
        var setup = """
        import pandas as pd
        source = pd.read_parquet('/data/source.parquet')
        """;
        runner.addSetupQuery(setup);
                     
        var op = """
        source.groupby(['str250', 'str640']).agg(
            Avg1=pd.NamedAgg('int250', "mean"), Avg2=pd.NamedAgg('int640', 'mean')
        )
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Average By", op, msize, rsize);
    }

}
