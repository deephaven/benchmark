package io.deephaven.benchmark.tests.standard.file;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

/**
 * Standard tests for writing/reading multi-column data with different codec/compression. To save time, the parquet
 * generated by the "write" tests is used by the "read" tests.
 */
@TestMethodOrder(OrderAnnotation.class)
@Tag("Iterate")
class S3ParquetMultiColTest {
    final String[] usedColumns = {"str10K", "long10K", "int10K", "short10K", "bigDec10K", "intArr5", "intVec5"};
    final FileTestRunner runner = new FileTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRequiredServices("deephaven", "minio");
        runner.setScaleFactors(3, 3);
    }

    @Test
    @Order(1)
    void writeMultiColNone() {
        runner.runParquetS3WriteTest("S3ParquetWrite- No Codec Multi Col -Static", usedColumns);
    }

    @Test
    @Order(2)
    void readMultiColNone() {
        runner.runParquetS3ReadTest("S3ParquetRead- No Codec Multi Col -Static");
    }

}
