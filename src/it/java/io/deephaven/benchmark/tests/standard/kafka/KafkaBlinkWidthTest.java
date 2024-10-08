/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Run a CountBy operation on Kafka blink data at different column widths
 */
public class KafkaBlinkWidthTest {
    final KafkaTestRunner runner = new KafkaTestRunner(this);
    final long rowCount = runner.api.propertyAsIntegral("scale.row.count", "100000");

    @Test
    void CountBy10ColsFromKafkaAvroBlink() {
        runner.api.setName("CountBy- 10 Cols Wide Avro Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount, 10, "long", "avro");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy10ColsFromKafkaJsonBlink() {
        runner.api.setName("CountBy- 10 Cols Wide JSON Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount, 10, "long", "json");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy10ColsFromKafkaProtobufBlink() {
        runner.api.setName("CountBy- 10 Cols Wide Protobuf Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 2, 10, "long", "protobuf");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy100ColsFromKafkaAvroBlink() {
        runner.api.setName("CountBy- 100 Cols Wide Avro Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 3, 100, "long", "avro");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy100ColsFromKafkaJsonBlink() {
        runner.api.setName("CountBy- 100 Cols Wide JSON Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 10, 100, "long", "json");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy100ColsFromKafkaProtobufBlink() {
        runner.api.setName("CountBy- 100 Cols Wide Protobuf Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 15, 100, "long", "protobuf");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy1000ColsFromKafkaAvroBlink() {
        runner.api.setName("CountBy- 1000 Cols Wide Avro Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 25, 1000, "long", "avro");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy1000ColsgFromKafkaJsonBlink() {
        runner.api.setName("CountBy- 1000 Cols Wide JSON Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 110, 1000, "long", "json");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @Test
    void CountBy1000ColsgFromKafkaProtobufBlink() {
        runner.api.setName("CountBy- 1000 Cols Wide Protobuf Blink -Inc");
        runner.restartWithHeap(10);
        runner.table(rowCount / 250, 1000, "long", "protobuf");
        runner.runTest("consumer_tbl.count_by('count')", "blink");
    }

    @AfterEach
    void teardown() {
        runner.api.close();
    }

}
