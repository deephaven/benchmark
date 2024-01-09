/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/**
 * Run a CountBy operation on a Kafka append consumer to compare different data types read from Kafka topics
 */
public class KafkaDataTypeTest {
    final KafkaTestRunner runner = new KafkaTestRunner(this);
    final long rowCount = runner.api.propertyAsIntegral("scale.row.count", "100000");
    final int colCount = 20;

    @Test
    void NoOp20LongColsFromKafkaAvroAppend() {
        runner.api.setName("NoOp- 20 Long Cols Avro Append");
        runner.restartWithHeap(10);
        runner.table(rowCount, colCount, "long", "avro");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20LongColsFromKafkaJsonAppend() {
        runner.api.setName("NoOp- 20 Long Cols JSON Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 2, colCount, "long", "json");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20LongColsFromKafkaProtobufAppend() {
        runner.api.setName("NoOp- 20 Long Cols Protobuf Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 3, colCount, "long", "protobuf");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20DoubleColsFromKafkaAvroAppend() {
        runner.api.setName("NoOp- 20 Double Cols Avro Append");
        runner.restartWithHeap(10);
        runner.table(rowCount, colCount, "double", "avro");
        runner.runTest("None", "append");
    }

    @Test
    void NoOpDoubleColsFromKafkaJsonAppend() {
        runner.api.setName("NoOp- 20 Double Cols JSON Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 4, colCount, "double", "json");
        runner.runTest("None", "append");
    }

    @Test
    void NoOpDoubleColsFromKafkaProtobufAppend() {
        runner.api.setName("NoOp- 20 Double Cols Protobuf Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 4, colCount, "double", "protobuf");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20DateTimeColsFromKafkaAvroAppend() {
        runner.api.setName("NoOp- 20 DateTime Cols Avro Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 2, colCount, "timestamp-millis", "avro");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20DateTimeColsFromKafkaJsonAppend() {
        runner.api.setName("NoOp- 20 DateTime Cols JSON Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 2, colCount, "timestamp-millis", "json");
        runner.runTest("None", "append");
    }

    @Test
    void NoOp20DateTimeColsFromKafkaProtobufAppend() {
        runner.api.setName("NoOp- 20 DateTime Cols Protobuf Append");
        runner.restartWithHeap(10);
        runner.table(rowCount / 8, colCount, "timestamp-millis", "protobuf");
        runner.runTest("None", "append");
    }

    @AfterEach
    void teardown() {
        runner.api.close();
    }

}
