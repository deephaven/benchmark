package io.deephaven.benchmark.tests.standard.parquet;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Duration;
import org.junit.jupiter.api.*;

/**
 * Standard tests for the parquet.write table operation. Writes raw generated table data to a parquet file for each
 * compression codec Deephaven supports
 */
public class WriteCompressedParquetTest {
    final ParquetTestSetup setup = new ParquetTestSetup(this);

    @Test
    public void writeZstd() {
        setup.table("compressed", "zstd");
        setup.api.setName("ParquetWrite- ZSTD 2 Strs 2 Longs 2 Dbls -Static");
        runTest("zstd");
    }

    @Test
    public void writeLz4() {
        setup.table("compressed", "lz4");
        setup.api.setName("ParquetWrite- LZ4 2 Strs 2 Longs 2 Dbls -Static");
        runTest("lz4");
    }

    @Test
    public void writeLzo() {
        setup.table("compressed", "lzo");
        setup.api.setName("ParquetWrite- LZO 2 Strs 2 Longs 2 Dbls -Static");
        runTest("lzo");
    }

    @Test
    public void writeGzip() {
        setup.table("compressed", "gzip");
        setup.api.setName("ParquetWrite- GZIP 2 Strs 2 Longs 2 Dbls -Static");
        runTest("gzip");
    }

    @Test
    public void writeSnappy() {
        setup.table("compressed", "snappy");
        setup.api.setName("ParquetWrite- SNAPPY 2 Strs 2 Longs 2 Dbls -Static");
        runTest("snappy");
    }

    @Test
    public void writeNone() {
        setup.api.setName("ParquetWrite- NONE Strs 2 Longs 2 Dbls -Static");
        runTest("none");
    }

    @AfterEach
    public void teardown() {
        setup.api.close();
    }

    private void runTest(String codec) {
        setup.table("compressed", codec);

        var query = """
        source = read("/data/compressed.parquet").select()
        write(source, '/data/compression.out.parquet', compression_codec_name='${codec}')  # Ignore 1st write time
        garbage_collect()
        
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        
        iterations = 2
        for i in range(1,iterations):
            write(source, '/data/compression.out.${codec}.parquet', compression_codec_name='${codec}')
        
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            float_col("elapsed_millis", [(end_time - begin_time) / 1000000.0 / iterations]),
            int_col("result_row_count", [source.size]),
        ])
        """;
        codec = codec.equals("none") ? "uncompressed" : codec;
        query = query.replace("${codec}", codec.toUpperCase());

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
