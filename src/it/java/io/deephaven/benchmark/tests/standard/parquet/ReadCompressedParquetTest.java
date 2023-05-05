package io.deephaven.benchmark.tests.standard.parquet;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.*;

/**
 * Standard tests for the parquet.read table operation. Read raw generated table data from a parquet file for each
 * compression codec Deephaven supports
 */
public class ReadCompressedParquetTest {
    final ParquetTestSetup setup = new ParquetTestSetup(this, 2);

    @Test
    public void readZstd() {
        setup.table("compressed", "zstd");
        setup.api.setName("ParquetRead- ZSTD 2 Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @Test
    public void readLz4() {
        setup.table("compressed", "lz4");
        setup.api.setName("ParquetRead- LZ4 2 Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @Test
    public void readLzo() {
        setup.table("compressed", "lzo");
        setup.api.setName("ParquetRead- LZO 2 Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @Test
    public void readGzip() {
        setup.table("compressed", "gzip");
        setup.api.setName("ParquetRead- GZIP 2 Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @Test
    public void readSnappy() {
        setup.table("compressed", "snappy");
        setup.api.setName("ParquetRead- SNAPPY 2 Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @Test
    public void readNone() {
        setup.table("compressed", "none");
        setup.api.setName("ParquetRead- NONE Strs 2 Longs 2 Dbls -Static");
        runTest();
    }

    @AfterEach
    public void teardown() {
        setup.api.close();
    }

    private void runTest() {
        var query = """
        result = read("/data/compressed.parquet").select()
        garbage_collect()
        
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()

        iterations = 9
        for i in range(1,iterations):
            result = read("/data/compressed.parquet").select()
        
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0 / iterations]),
            int_col("result_row_count", [result.size]),
        ])
        """;

        setup.api.query(query).fetchAfter("stats", table -> {
            long elapsedMillis = table.getSum("elapsed_millis").intValue();
            long rcount = table.getSum("result_row_count").longValue();
            assertEquals(setup.scaleRowCount, rcount, "Wrong loaded row count");
            setup.api.result().test("deephaven-engine", Duration.ofMillis(elapsedMillis), setup.scaleRowCount);
        }).fetchAfter("standard_metrics", table -> {
            setup.api.metrics().add(table);
        }).execute();
    }

}
