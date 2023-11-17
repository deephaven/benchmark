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
        runner.setScaleFactors(5, 3);
        runner.runCsvWriteTest("CsvWrite- 3 Integral Cols -Static", "short10K", "int10K", "long10K");
    }

    @Test
    @Order(2)
    void readThreeIntegralCols() {
        runner.setScaleFactors(5, 3);
        runner.runCsvReadTest("CsvRead- 3 Integral Cols -Static", "short10K", "int10K", "long10K");
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
