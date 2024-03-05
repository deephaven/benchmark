/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the partitionBy table operation. Divides a single table into subtables.
 */
@Disabled
public class PartitionByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.tables("source");
    }

    @Test
    public void partitionBy1Group2Cols() {
        var q = "source.partition_by(['key1']).get_constituent(['string1val'])";
        runner.test("PartitionBy- 1 Group 100 Unique Vals", 1, q, "key1", "num1");
    }

    // @Test
    // public void partitionBy1Group2ColsLarge() {
    // var q = "source.partition_by(['str3']).get_constituent(['val1string'])";
    // runner.test("PartitionBy- 1 Group 1M Unique Vals", 1000000, q, "str3", "int1M");
    // }
    //
    // @Test
    // public void partitionBy2Group3Cols() {
    // var q = "source.partition_by(by=['str1', 'str2'])";
    // runner.test("PartitionBy- 2 Group 160K Unique Combos", 160000, q, "str1", "str2", "int250");
    // }

}
