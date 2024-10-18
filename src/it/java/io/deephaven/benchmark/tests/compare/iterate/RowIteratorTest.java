/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare.iterate;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.compare.CompareTestRunner;

/**
 * Product comparison tests for iterating and summing table columns. Tests read the same parquet data. To avoid an
 * unfair advantage where some products may partition or group data during the read, parquet read time is included in
 * the benchmark results.
 * <p>
 * Each test produces a table result containing one row with one column that is the total of the result of the sum of
 * two columns for each row. ex. sum((r1c1 + r1c2)..(rNc1 + rNc2)). This is achieved without creating an extra column to
 * hold the column sums.
 * <p>
 * Data generation only happens in the first test, the Deephaven test. Tests can be run individually, but only after the
 * desired data has been generated.
 */
@TestMethodOrder(OrderAnnotation.class)
public class RowIteratorTest {
    final CompareTestRunner runner = new CompareTestRunner(this);

    @Test
    @Order(1)
    public void deephavenRowIterator() {
        runner.initDeephaven(2, "source", null, "int250", "int640");
        var setup = "from deephaven.parquet import read";
        var op = """
        source = read('/data/source.parquet').select()
        result = new_table([
            long_col('total', [sum(row.int250 + row.int640 for row in source.iter_tuple())])
        ])
        """;
        var msize = "source.size";
        var rsize = "result.size";
        runner.test("Deephaven Row Iterator", setup, op, msize, rsize);
    }

    @Test
    @Order(2)
    public void pyarrowRowIterator() {
        runner.initPython("pyarrow");
        var setup = """
        import pyarrow as pa
        import pyarrow.dataset as ds
        import pyarrow.compute as pc
        
        def iterdicts(table, cols=[]):
            for batch in table.to_batches(1024):
                d = batch.to_pydict()
                int250 = d['int250']
                int640 = d['int640']
                for i in range(len(int250)):
                    row = {'int250':int250[i],'int640':int640[i]}
                    yield row
        """;
        var op = """
        source = ds.dataset('/data/source.parquet', format="parquet").to_table()
        rsum = sum(row['int250'] + row['int640'] for row in iterdicts(source))
        result = pa.Table.from_pydict({'total':[rsum]})
        """;
        var msize = "source.num_rows";
        var rsize = "result.num_rows";
        runner.test("PyArrow Row Iterator", setup, op, msize, rsize);
    }

    @Test
    @Order(3)
    public void pandasRowIteratior() {
        runner.initPython("fastparquet", "pandas");
        var setup = "import pandas as pd";
        var op = """
        source = pd.read_parquet('/data/source.parquet')
        rsum = sum(row.int250 + row.int640 for row in source.itertuples())
        result = pd.DataFrame([[rsum]], columns=['total'])
        """;
        var msize = "len(source)";
        var rsize = "len(result)";
        runner.test("Pandas Row Iterator", setup, op, msize, rsize);
    }

    @Test
    @Order(4)
    public void duckdbRowIterator() {
        runner.initPython("duckdb");
        var setup = """
        import duckdb as db

        def iterdicts(table):
            while batch := table.fetchmany(1024):
                for row in batch:
                    r = {'int250':row[0],'int640':row[1]}
                    yield r
        """;
        var op = """
        source = db.sql("SELECT * FROM '/data/source.parquet'")
        
        db.sql("CREATE TABLE results(total INT)")
        rsum = sum(row['int250'] + row['int640'] for row in iterdicts(source))
        db.sql("INSERT INTO results VALUES(" + str(rsum) + ")")
        sourceLen = db.sql("SELECT count(*) FROM source").fetchone()[0]
        resultLen = db.sql("SELECT count(*) FROM results").fetchone()[0]
        """;
        var msize = "sourceLen";
        var rsize = "resultLen";
        runner.test("DuckDb Row Iterator", setup, op, msize, rsize);
    }

}
