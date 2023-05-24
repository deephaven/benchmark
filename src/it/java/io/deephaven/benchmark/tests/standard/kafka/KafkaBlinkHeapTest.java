/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Run CountBy operation on Kafka blink data at different heap settings. Some tests have a heap that is smaller than the
 * incoming data, and some have heaps that are larger.
 */
public class KafkaBlinkHeapTest {
    final KafkaTestRunner runner = new KafkaTestRunner(this);
    final long rowCount = runner.api.propertyAsIntegral("scale.row.count", "100000") * 2;
    final int colCount = 20;

    @Test
    public void CountBy1gHeapFromKafkaAvroBlink() {
        runner.api.setName("CountBy- 20 Cols 1g Heap Avro Blink");
        runner.restartWithHeap(1);
        runner.table(rowCount / 2, colCount, "long", "avro");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    public void CountBy1gHeapFromKafkaJsonBlink() {
        runner.api.setName("CountBy- 20 Cols 1g Heap JSON Blink");
        runner.restartWithHeap(1);
        runner.table(rowCount / 4, colCount, "long", "json");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    public void CountBy10gHeapFromKafkaAvroBlink() {
        runner.api.setName("CountBy- 20 Cols 10g Heap Avro Blink");
        runner.restartWithHeap(10);
        runner.table(rowCount / 2, colCount, "long", "avro");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    public void CountBy10gHeapFromKafkaJsonBlink() {
        runner.api.setName("CountBy- 20 Cols 10g Heap JSON Blink");
        runner.restartWithHeap(10);
        runner.table(rowCount / 4, colCount, "long", "json");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @AfterEach
    public void teardown() {
        runner.api.close();
    }

}
