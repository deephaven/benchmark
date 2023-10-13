package io.deephaven.benchmark.tests.standard.parquet;

import org.junit.jupiter.api.*;

/**
 * Standard tests for writing single column parquet for different column types.
 */
class ParquetSingleColTest {
    final ParquetTestRunner runner = new ParquetTestRunner(this);

    @Test
    void writeOneStringCol() {
        runner.setScaleFactors(5, 10);
        runner.runWriteTest("ParquetWrite- 1 String Col -Static", "SNAPPY", "str10K");
    }

    @Test
    void writeOneBigDecimalCol() {
        runner.setScaleFactors(5, 3);
        runner.runWriteTest("ParquetWrite- 1 Big Decimal Col -Static", "SNAPPY", "bigDec10K");
    }

    @Test
    void writeOneLongCol() {
        runner.setScaleFactors(5, 10);
        runner.runWriteTest("ParquetWrite- 1 Long Col -Static", "SNAPPY", "long10K");
    }

    @Test
    void writeOneIntCol() {
        runner.setScaleFactors(5, 20);
        runner.runWriteTest("ParquetWrite- 1 Int Col -Static", "SNAPPY", "int10K");
    }

    @Test
    void writeOneShortCol() {
        runner.setScaleFactors(5, 20);
        runner.runWriteTest("ParquetWrite- 1 Short Col -Static", "SNAPPY", "short10K");
    }

    @Test
    void writeOneInt1KArrayCol() {
        runner.setScaleFactors(0.10, 2);
        runner.runWriteTest("ParquetWrite- 1 Array Col of 1K Ints -Static", "SNAPPY", "intArr1K");
    }

    @Test
    void writeOneInt1KVectorCol() {
        runner.setScaleFactors(0.10, 2);
        runner.runWriteTest("ParquetWrite- 1 Vector Col of 1K Ints -Static", "SNAPPY", "intVec1K");
    }
    
    @Test
    void writeOneInt5ArrayCol() {
        runner.setScaleFactors(2, 10);
        runner.runWriteTest("ParquetWrite- 1 Array Col of 5 Ints -Static", "SNAPPY", "intArr5");
    }

    @Test
    void writeOneInt5VectorCol() {
        runner.setScaleFactors(2, 10);
        runner.runWriteTest("ParquetWrite- 1 Vector Col of 5 Ints -Static", "SNAPPY", "intVec5");
    }

    @Test
    void writeOneObjectArrayCol() {
        runner.setScaleFactors(2, 1);
        runner.runWriteTest("ParquetWrite- 1 Array Col of 3 Strings and 2 Nulls -Static", "SNAPPY", "objArr5");
    }

    @Test
    void writeOneObjectVectorCol() {
        runner.setScaleFactors(1, 1);
        runner.runWriteTest("ParquetWrite- 1 Vector Col of 3 String and 2 Nulls -Static", "SNAPPY", "objVec5");
    }

}
