package io.deephaven.benchmark.tests.standard.parquet;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;

/**
 * Standard tests for writing/reading single column parquet for different column types. To save
 * time, the parquet generated by the "write" tests is used by the "read" tests
 */

//Need to add array and vector tests.  Look at adding iterations

@TestMethodOrder(OrderAnnotation.class)
public class ParquetSingleColTest {
    final ParquetTestRunner runner = new ParquetTestRunner(this);

    @Test
    @Order(1)
    public void writeOneStringCol() {
        runner.runWriteTest("ParquetWrite- 1 String Col -Static", "SNAPPY", "str10K");
    }
    
    @Test
    @Order(2)
    public void readOneStringCol() {
        runner.runReadTest("ParquetRead- 1 String Col -Static", "SNAPPY", "str10K");
    }
    
    @Test
    @Order(3)
    public void writeOneBigDecimalCol() {
        runner.runWriteTest("ParquetWrite- 1 Big Decimal Col -Static", "SNAPPY", "bigDec10K");
    }
    
    @Test
    @Order(4)
    public void readOneBigDecimal10KCol() {
        runner.runReadTest("ParquetRead- 1 Big Decimal Col -Static", "SNAPPY", "bigDec10K");
    }
    
    @Test
    @Order(5)
    public void writeOneLongCol() {
        runner.runWriteTest("ParquetWrite- 1 Long Col -Static", "SNAPPY", "long10K");
    }
    
    @Test
    @Order(6)
    public void readOneLongCol() {
        runner.runReadTest("ParquetRead- 1 Long Col -Static", "SNAPPY", "long10K");
    }
    
    @Test
    @Order(7)
    public void writeOneIntCol() {
        runner.runWriteTest("ParquetWrite- 1 Int Col -Static", "SNAPPY", "int10K");
    }
    
    @Test
    @Order(8)
    public void readOneIntCol() {
        runner.runReadTest("ParquetRead- 1 Int Col -Static", "SNAPPY", "int10K");
    }
    
    @Test
    @Order(9)
    public void writeOneShortCol() {
        runner.runWriteTest("ParquetWrite- 1 Short Col -Static", "SNAPPY", "short10K");
    }

    @Test
    @Order(10)
    public void readOneShortCol() {
        runner.runReadTest("ParquetRead- 1 Short Col -Static", "SNAPPY", "short10K");
    }

}
