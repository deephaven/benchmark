/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.by;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the partitionBy table operation. Divides a single table into subtables.
 */
public class PartitionByTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source");
    }

    @Test
    void partitionBy1Group() {
        runner.setScaleFactors(44, 9);
        var q = "source.partition_by(by=['key1']).table";
        runner.test("PartitionBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void partitionBy2Groups() {
        runner.setScaleFactors(8, 4);
        var q = "source.partition_by(by=['key1', 'key2']).table";
        runner.test("PartitionBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void partitionBy3Groups() {
        runner.setScaleFactors(4, 2);
        var q = "source.partition_by(by=['key1', 'key2', 'key3']).table";
        runner.test("PartitionBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
