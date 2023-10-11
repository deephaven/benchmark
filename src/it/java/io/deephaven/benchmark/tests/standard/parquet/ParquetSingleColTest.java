package io.deephaven.benchmark.tests.standard.parquet;

import org.junit.jupiter.api.*;

/**
 * Standard tests for writing single column parquet for different column types.
 */
public class ParquetSingleColTest {
    final ParquetTestRunner runner = new ParquetTestRunner(this);

    @Test
    public void writeOneStringCol() {
        runner.setScaleFactors(5, 15);
        runner.runWriteTest("ParquetWrite- 1 String Col -Static", "SNAPPY", "str10K");
    }

    @Test
    public void writeOneBigDecimalCol() {
        runner.setScaleFactors(5, 6);
        runner.runWriteTest("ParquetWrite- 1 Big Decimal Col -Static", "SNAPPY", "bigDec10K");
    }

    @Test
    public void writeOneLongCol() {
        runner.setScaleFactors(5, 15);
        runner.runWriteTest("ParquetWrite- 1 Long Col -Static", "SNAPPY", "long10K");
    }

    @Test
    public void writeOneIntCol() {
        runner.setScaleFactors(5, 30);
        runner.runWriteTest("ParquetWrite- 1 Int Col -Static", "SNAPPY", "int10K");
    }

    @Test
    public void writeOneShortCol() {
        runner.setScaleFactors(5, 35);
        runner.runWriteTest("ParquetWrite- 1 Short Col -Static", "SNAPPY", "short10K");
    }

    @Test
    public void writeOneArrayCol() {
        runner.setScaleFactors(0.10, 1);
        runner.runWriteTest("ParquetWrite- 1 Int Array Col -Static", "SNAPPY", "array1K");
    }

    @Test
    public void writeOneVectorCol() {
        runner.setScaleFactors(0.10, 1);
        runner.runWriteTest("ParquetWrite- 1 Int Vector Col -Static", "SNAPPY", "vector1K");
    }

}
