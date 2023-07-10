/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import static org.apache.kafka.clients.producer.ProducerConfig.*;
import java.util.Properties;
import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringSerializer;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Threads;

/**
 * Generator that produces rows to a Kafka topic according to the provided column definitions. This generator uses JSON
 * string formatting and requires a JSON spec to be provided in the consumer to define the types for column parsing.
 */
public class JsonKafkaGenerator implements Generator {
    final private ExecutorService queue = Threads.single("JsonKafkaGenerator");
    final private Producer<String, String> producer;
    final private KafkaAdmin admin;
    final private ColumnDefs columnDefs;
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
    public JsonKafkaGenerator(String bootstrapServers, String schemaRegistryUrl, String topic, ColumnDefs columnDefs,
            String compression) {
        cleanupTopic(bootstrapServers, schemaRegistryUrl, topic);
        this.producer = createProducer(bootstrapServers, schemaRegistryUrl, compression);
        this.admin = new KafkaAdmin(bootstrapServers, schemaRegistryUrl);
        this.topic = topic;
        this.columnDefs = columnDefs;
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
                var colNames = new ArrayList<>(columnDefs.toTypeMap().keySet());
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
                        StringBuilder json = new StringBuilder("{ ");
                        for (int i = 0, n = columnDefs.getCount(); i < n; i++) {
                            if (i > 0)
                                json.append(',');
                            append(json, colNames.get(i)).append(':');
                            append(json, columnDefs.nextValue(i, recCount, maxRecordCount));
                        }
                        json.append('}');
                        producer.send(new ProducerRecord<>(topic, json.toString()));
                        if (perRecordPauseMillis <= 0)
                            Thread.yield();
                        else
                            Threads.sleep(perRecordPauseMillis);

                        ++recCount;
                        duration = System.currentTimeMillis() - beginTime;
                        if (duration > maxDuration)
                            isDone = true;
                    } catch (Exception ex) {
                        throw new RuntimeException("Failed to send to topic: " + topic, ex);
                    }
                }
                producer.flush();
                Metrics metrics = new Metrics("test-runner", topic, "generator").set("duration.secs", duration / 1000.0)
                        .set("record.count", recCount).set("send.rate", recCount / (duration / 1000.0))
                        .set("verified.record.count", admin.getMessageCount(topic));
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

    private Producer<String, String> createProducer(String bootstrapServer, String schemaRegistryUrl,
            String compression) {
        Properties props = new Properties();
        props.put(BOOTSTRAP_SERVERS_CONFIG, bootstrapServer);
        props.put(KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        props.put(VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
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

    private StringBuilder append(StringBuilder str, Object val) {
        if (val instanceof CharSequence)
            str.append('"').append(val).append('"');
        else
            str.append(val);
        return str;
    }

}
