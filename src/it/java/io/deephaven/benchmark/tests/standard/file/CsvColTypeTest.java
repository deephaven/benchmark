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
    void writeFourIntegralCols() {
        runner.setScaleFactors(5, 2);
        runner.runCsvWriteTest("CsvWrite- 4 Integral Cols -Static", "byte100", "short10K", "int10K", "long10K");
    }

    @Test
    @Order(2)
    void readFourIntegralCols() {
        runner.setScaleFactors(5, 2);
        runner.runCsvReadTest("CsvRead- 4 Integral Cols -Static", "byte100", "short10K", "int10K", "long10K");
    }

    @Test
    @Order(3)
    void writeOneStringCol() {
        runner.setScaleFactors(5, 5);
        runner.runCsvWriteTest("CsvWrite- 1 String Col -Static", "str10K");
    }

    @Test
    @Order(4)
    void readOneStringCol() {
        runner.setScaleFactors(5, 5);
        runner.runCsvReadTest("CsvRead- 1 String Col -Static", "str10K");
    }

}
