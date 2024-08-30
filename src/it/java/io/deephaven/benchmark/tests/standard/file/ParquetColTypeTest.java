package io.deephaven.benchmark.tests.standard.file;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

/**
 * Standard tests for writing single column parquet for different column types.
 */
@TestMethodOrder(OrderAnnotation.class)
@Tag("Iterate")
class ParquetColTypeTest {
    final FileTestRunner runner = new FileTestRunner(this);

    void setup(double rowFactor, int scaleFactor) {
        runner.setScaleFactors(rowFactor, scaleFactor);
    }
    
    @Test
    @Order(1)
    void writeFourIntegralCols() {
        setup(5, 12);
        runner.runParquetWriteTest("ParquetWrite- 4 Integral Cols -Static", "NONE", "byte100", "short10K", "int10K",
                "long10K");
    }

    @Test
    @Order(2)
    void readFourIntegralCols() {
        setup(5, 12);
        runner.runParquetReadTest("ParquetRead- 4 Integral Cols -Static");
    }

    @Test
    @Order(3)
    void writeOneStringCol() {
        setup(5, 30);
        runner.runParquetWriteTest("ParquetWrite- 1 String Col -Static", "NONE", "str10K");
    }

    @Test
    @Order(4)
    void readOneStringCol() {
        setup(5, 30);
        runner.runParquetReadTest("ParquetRead- 1 String Col -Static");
    }

    @Test
    @Order(5)
    @Tag("Iterate")
    void writeOneBigDecimalCol() {
        setup(5, 5);
        runner.runParquetWriteTest("ParquetWrite- 1 Big Decimal Col -Static", "NONE", "bigDec10K");
    }

    @Test
    @Order(6)
    @Tag("Iterate")
    void readOneBigDecimalCol() {
        setup(5, 5);
        runner.runParquetReadTest("ParquetRead- 1 Big Decimal Col -Static");
    }

    @Test
    @Order(7)
    void writeOneInt1KArrayCol() {
        setup(0.10, 2);
        runner.runParquetWriteTest("ParquetWrite- 1 Array Col of 1K Ints -Static", "NONE", "intArr1K");
    }

    @Test
    @Order(8)
    void readOneInt1KArrayCol() {
        setup(0.10, 2);
        runner.runParquetReadTest("ParquetRead- 1 Array Col of 1K Ints -Static");
    }

    @Test
    @Order(9)
    void writeOneInt1KVectorCol() {
        setup(0.10, 2);
        runner.runParquetWriteTest("ParquetWrite- 1 Vector Col of 1K Ints -Static", "NONE", "intVec1K");
    }

    @Test
    @Order(10)
    void readOneInt1KVectorCol() {
        setup(0.10, 2);
        runner.runParquetReadTest("ParquetRead- 1 Vector Col of 1K Ints -Static");
    }

    @Test
    @Order(11)
    void writeOneInt5ArrayCol() {
        setup(2, 20);
        runner.runParquetWriteTest("ParquetWrite- 1 Array Col of 5 Ints -Static", "NONE", "intArr5");
    }

    @Test
    @Order(12)
    void readOneInt5ArrayCol() {
        setup(2, 20);
        runner.runParquetReadTest("ParquetRead- 1 Array Col of 5 Ints -Static");
    }

    @Test
    @Order(13)
    void writeOneInt5VectorCol() {
        setup(2, 16);
        runner.runParquetWriteTest("ParquetWrite- 1 Vector Col of 5 Ints -Static", "NONE", "intVec5");
    }

    @Test
    @Order(14)
    void readOneInt5VectorCol() {
        setup(2, 16);
        runner.runParquetReadTest("ParquetRead- 1 Vector Col of 5 Ints -Static");
    }

    @Test
    @Order(15)
    void writeOneObjectArrayCol() {
        setup(2, 1);
        runner.runParquetWriteTest("ParquetWrite- 1 Array Col of 3 Strings and 2 Nulls -Static", "NONE", "objArr5");
    }

    @Test
    @Order(16)
    void readOneObjectArrayCol() {
        setup(2, 1);
        runner.runParquetReadTest("ParquetRead- 1 Array Col of 3 Strings and 2 Nulls -Static");
    }

    @Test
    @Order(17)
    void writeOneObjectVectorCol() {
        setup(1, 1);
        runner.runParquetWriteTest("ParquetWrite- 1 Vector Col of 3 String and 2 Nulls -Static", "NONE", "objVec5");
    }

    @Test
    @Order(18)
    void readOneObjectVectorCol() {
        setup(1, 1);
        runner.runParquetReadTest("ParquetRead- 1 Vector Col of 3 String and 2 Nulls -Static");
    }

}
