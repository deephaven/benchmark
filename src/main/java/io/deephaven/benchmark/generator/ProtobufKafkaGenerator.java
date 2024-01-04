/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import static com.google.protobuf.util.Timestamps.fromMillis;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import com.google.protobuf.DynamicMessage;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.protobuf.ProtobufSchema;
import io.confluent.kafka.serializers.protobuf.KafkaProtobufSerializer;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Log;
import io.deephaven.benchmark.util.Threads;

/**
 * Generator that produces rows to a Kafka topic according to the provided column definitions. The generator uses
 * Protobuf formatting and automatically generates and publishes the correct schema for use by the producer and
 * consumer.
 */
public class ProtobufKafkaGenerator implements Generator {
    final private ExecutorService queue = Threads.single("ProtobufKafkaGenerator");
    final private Producer<String, DynamicMessage> producer;
    final private ColumnDefs columnDefs;
    final private ProtobufSchema schema;
    final private String topic;
    final private AtomicBoolean isClosed = new AtomicBoolean(false);

    /**
     * Initialize with kafka server and schema registry locations, kafka topic, column definitions, and compression
     * scheme
     * 
     * @param bootstrapServers the kafka external location (ex. localhost:9092)
     * @param schemaRegistryUrl the ReST schema registry location (ex. localhost:8081)
     * @param topic the kafka topic to produce record to (ex. mytable)
     * @param columnDefs the column definitions specifying what the data looks like
     * @param compression one of Kafka's <code>ProducerConfig.COMPRESSION_TYPE_CONFIG</code> schemes
     */
    public ProtobufKafkaGenerator(String bootstrapServers, String schemaRegistryUrl, String topic,
            ColumnDefs columnDefs,
            String compression) {
        cleanupTopic(bootstrapServers, schemaRegistryUrl, topic);
        this.producer = createProducer(bootstrapServers, schemaRegistryUrl, compression);
        this.topic = topic;
        this.columnDefs = columnDefs;
        this.schema = publishSchema(topic, schemaRegistryUrl, getSchemaMessage(topic, columnDefs));
    }

    /**
     * Produce a maximum number of records to a Kafka topic asynchronously.
     * 
     * @param perRecordPauseMillis wait time between each record sent
     * @param maxRecordCount maximum records to produce
     * @param maxDurationSecs maximum duration to produce (May prevent maximum records from being produces)
     */
    public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs) {
        checkClosed();
        var r = new Callable<Metrics>() {
            @Override
            public Metrics call() {
                final long maxDuration = maxDurationSecs * 1000;
                final long beginTime = System.currentTimeMillis();
                long recCount = 0;
                long duration = 0;
                boolean isDone = false;
                while (!isClosed.get() && !isDone) {
                    try {
                        if (recCount >= maxRecordCount) {
                            isDone = true;
                            continue;
                        }
                        var msgBuilder = schema.newMessageBuilder(topic);
                        var fields = msgBuilder.getDescriptorForType().getFields();
                        for (int i = 0, n = columnDefs.getCount(); i < n; i++) {
                            var v = columnDefs.nextValue(i, recCount, maxRecordCount);
                            var field = fields.get(i);
                            v = field.toProto().getTypeName().contains("Timestamp") ? fromMillis((Long) v) : v;
                            msgBuilder.setField(field, v);
                        }
                        producer.send(new ProducerRecord<>(topic, msgBuilder.build()));
                        if (perRecordPauseMillis <= 0)
                            Thread.yield();
                        else
                            Threads.sleep(perRecordPauseMillis);

                        if (++recCount % 10_000_000 == 0)
                            Log.info("Produced %s records to topic '%s'", recCount, topic);
                        duration = System.currentTimeMillis() - beginTime;
                        if (duration > maxDuration)
                            isDone = true;
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to send to topic: " + topic, ex);
                    }
                }
                Log.info("Produced %s records to topic: %s", recCount, topic);
                Metrics metrics = new Metrics("test-runner", topic, "generator").set("duration.secs", duration / 1000.0)
                        .set("record.count", recCount).set("send.rate", recCount / (duration / 1000.0));
                return metrics;
            }
        };
        return queue.submit(r);
    }

    /**
     * Close the producer and shutdown any async threads created during production
     */
    public void close() {
        if (isClosed.get())
            return;
        isClosed.set(true);
        queue.shutdown();
        producer.flush();
        producer.close();
    }

    private void checkClosed() {
        if (isClosed.get())
            throw new RuntimeException("Generator is closed");
    }

    private Producer<String, DynamicMessage> createProducer(String bootstrapServer, String schemaRegistryUrl,
            String compression) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, KafkaProtobufSerializer.class);
        props.put("schema.registry.url", schemaRegistryUrl);
        props.put(ACKS_CONFIG, "0");
        props.put(COMPRESSION_TYPE_CONFIG, getCompression(compression));
        props.put(BATCH_SIZE_CONFIG, 16384 * 4);
        props.put(BUFFER_MEMORY_CONFIG, 32 * 1024 * 1024L * 4);
        props.put(LINGER_MS_CONFIG, 200);
        return new KafkaProducer<>(props);
    }

    private String getCompression(String codec) {
        codec = codec.toLowerCase();
        switch (codec) {
            case "none", "gzip", "snappy", "lz4", "zstd":
                return codec;
            default:
                return "snappy";
        }
    }

    private void cleanupTopic(String bootstrapServers, String schemaRegistryUrl, String topic) {
        var admin = new KafkaAdmin(bootstrapServers, schemaRegistryUrl);
        admin.deleteTopic(topic);
        long messageCount = admin.getMessageCount(topic);
        if (messageCount > 0)
            throw new RuntimeException("Failed to delete topic: " + topic + "=" + messageCount + " msgs");
    }

    private ProtobufSchema publishSchema(String topic, String schemaRegistryUrl, String schemaProto) {
        try {
            ProtobufSchema schema = new ProtobufSchema(schemaProto);
            CachedSchemaRegistryClient client = new CachedSchemaRegistryClient(schemaRegistryUrl, 20);
            String subject = topic + "_record";
            String subject2 = topic + "-value";
            Collection<String> subjects = client.getAllSubjects();
            deleteSubjectIfExists(client, subjects, subject);
            deleteSubjectIfExists(client, subjects, subject2);
            client.register(subject, schema);
            return schema;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to publish schema: " + schemaRegistryUrl, ex);
        }
    }

    private void deleteSubjectIfExists(CachedSchemaRegistryClient client, Collection<String> subjects, String subject)
            throws Exception {
        if (subjects.stream().anyMatch(s -> s.equalsIgnoreCase(subject))) {
            client.deleteSubject(subject, false);
            client.deleteSubject(subject, true);
        }
    }

    private String getSchemaMessage(String topic, ColumnDefs fieldDefs) {
        var schema = """
        syntax = "proto3";
        
        import "google/protobuf/timestamp.proto";

        message ${topic} {
            ${fields}
        }
        """;
        var fields = "";
        int count = 0;
        for (Map.Entry<String, String> e : fieldDefs.toTypeMap().entrySet()) {
            var name = e.getKey();
            var type = e.getValue();
            fields += getFieldType(type) + ' ' + name + " = " + ++count + ";\n";
        }
        schema = schema.replace("${topic}", topic);
        return schema.replace("${fields}", fields);
    }

    private String getFieldType(String type) {
        return switch (type) {
            case "long" -> "int64";
            case "int" -> "int32";
            case "double" -> "double";
            case "float" -> "float";
            case "string" -> "string";
            case "timestamp-millis" -> "google.protobuf.Timestamp";
            default -> throw new RuntimeException("Unsupported generator data type: " + type);
        };
    }

}
