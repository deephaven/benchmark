package io.deephaven.benchmark.tests.standard.file;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

/**
 * Standard tests for writing single column CSV for different column types.
 */
@TestMethodOrder(OrderAnnotation.class)
class CsvColTypeTest {
    final FileTestRunner runner = new FileTestRunner(this);

    @Test
    @Order(1)
    void writeThreeIntegralCols() {
        runner.setScaleFactors(5, 2);
        runner.runCsvWriteTest("CsvWrite- 3 Long Cols -Static", "short10K", "int10K", "long10K");
    }

    @Test
    @Order(2)
    void readThreeIntegralCols() {
        runner.setScaleFactors(5, 2);
        runner.runCsvReadTest("CsvRead- 3 Long Cols -Static");
    }

    @Test
    @Order(3)
    void writeOneStringCol() {
        runner.setScaleFactors(5, 10);
        runner.runCsvWriteTest("CsvWrite- 1 String Col -Static", "str10K");
    }

    @Test
    @Order(4)
    void readOneStringCol() {
        runner.setScaleFactors(5, 10);
        runner.runCsvReadTest("CsvRead- 1 String Col -Static");
    }

    @Test
    @Order(5)
    void writeOneBigDecimalCol() {
        runner.setScaleFactors(5, 10);
        runner.runCsvWriteTest("CsvWrite- 1 Big Decimal Col -Static", "bigDec10K");
    }

    @Test
    @Order(6)
    void readOneBigDecimalCol() {
        runner.setScaleFactors(5, 10);
        runner.runCsvReadTest("CsvRead- 1 Big Decimal Col -Static");
    }

    @Test
    @Order(7)
    void writeOneInt1KArrayCol() {
        runner.setScaleFactors(0.10, 200);
        runner.runCsvWriteTest("CsvWrite- 1 Array Col of 1K Ints -Static", "intArr1K");
    }

    @Test
    @Order(8)
    void readOneInt1KArrayCol() {
        runner.setScaleFactors(0.10, 200);
        runner.runCsvReadTest("CsvRead- 1 Array Col of 1K Ints -Static");
    }

    @Test
    @Order(9)
    void writeOneInt1KVectorCol() {
        runner.setScaleFactors(0.10, 100);
        runner.runCsvWriteTest("CsvWrite- 1 Vector Col of 1K Ints -Static", "intVec1K");
    }

    @Test
    @Order(10)
    void readOneInt1KVectorCol() {
        runner.setScaleFactors(0.10, 100);
        runner.runCsvReadTest("CsvRead- 1 Vector Col of 1K Ints -Static");
    }

    @Test
    @Order(11)
    void writeOneInt5ArrayCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvWriteTest("CsvWrite- 1 Array Col of 5 Ints -Static", "intArr5");
    }

    @Test
    @Order(12)
    void readOneInt5ArrayCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvReadTest("CsvRead- 1 Array Col of 5 Ints -Static");
    }

    @Test
    @Order(13)
    void writeOneInt5VectorCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvWriteTest("CsvWrite- 1 Vector Col of 5 Ints -Static", "intVec5");
    }

    @Test
    @Order(14)
    void readOneInt5VectorCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvReadTest("CsvRead- 1 Vector Col of 5 Ints -Static");
    }

    @Test
    @Order(15)
    void writeOneObjectArrayCol() {
        runner.setScaleFactors(2, 8);
        runner.runCsvWriteTest("CsvWrite- 1 Array Col of 3 Strings and 2 Nulls -Static", "objArr5");
    }

    @Test
    @Order(16)
    void readOneObjectArrayCol() {
        runner.setScaleFactors(2, 8);
        runner.runCsvReadTest("CsvRead- 1 Array Col of 3 Strings and 2 Nulls -Static");
    }

    @Test
    @Order(17)
    void writeOneObjectVectorCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvWriteTest("CsvWrite- 1 Vector Col of 3 String and 2 Nulls -Static", "objVec5");
    }

    @Test
    @Order(18)
    void readOneObjectVectorCol() {
        runner.setScaleFactors(2, 10);
        runner.runCsvReadTest("CsvRead- 1 Vector Col of 3 String and 2 Nulls -Static");
    }

}
