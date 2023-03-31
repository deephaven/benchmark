/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.Properties;
import java.util.concurrent.Future;
import org.apache.avro.generic.GenericRecord;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.common.serialization.StringSerializer;
import io.deephaven.benchmark.metric.Metrics;

public class JsonKafkaGenerator implements Generator {
    final private Producer<String, GenericRecord> producer;
    final private ColumnDefs fieldDefs;
    final private String topic;

    // bootstrapServers - "redpanda:29092"
    public JsonKafkaGenerator(String bootstrapServer, String topic, ColumnDefs columnDefs) {
        Properties props = new Properties();
        props.put("bootstrap.servers", bootstrapServer);
        props.put("key.serializer", StringSerializer.class);
        props.put("value.serializer", StringSerializer.class);
        props.put("acks", "0");
        this.producer = new KafkaProducer<>(props);
        this.fieldDefs = columnDefs;
        this.topic = topic;

    }

    @Override
    public Future<Metrics> produce(int perRecordPauseSecs, long maxRecordCount, int maxDurationSecs) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub

    }

}
