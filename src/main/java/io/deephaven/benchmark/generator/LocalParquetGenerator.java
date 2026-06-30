/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import blue.strategic.parquet.*;
import org.apache.parquet.schema.MessageType;
import org.apache.parquet.schema.MessageTypeParser;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Log;
import io.deephaven.benchmark.util.Threads;

/**
 * Generator that produces rows to a local Parquet file according to the provided column definitions.<pr/> Note: This
 * generator MUST generate the same row and column data in the same order and types as the non-local
 * <code>AvroKafkaGenerator</code> when the two generators have the same column definitions. (The "same data" is defined
 * by how it looks in Deephaven tables, not byte-for-byte in the files.)
 */
public class LocalParquetGenerator implements Generator {
    final private ExecutorService queue = Threads.single("LocalParquetGenerator");
    final private ColumnDefs columnDefs;
    final private String topic;
    final private long startSeed;
    final private MessageType schema;
    final private File parquetFile;
    final private AtomicBoolean isClosed = new AtomicBoolean(false);
    private ParquetWriter<Object[]> writer;

    /**
     * Create a local Parquet generator with the provided column definitions and output file. The column definitions
     * determine the schema of the Parquet file and the data generated for each column.
     *
     * @param parquetFile output Parquet file path
     * @param topic topic name (used for logging and schema generation)
     * @param columnDefs column definitions that determine the schema and generated data
     * @param startSeed starting seed for data generation
     */
    public LocalParquetGenerator(String parquetFile, String topic, ColumnDefs columnDefs, long startSeed) {
        this.topic = topic;
        this.columnDefs = columnDefs;
        this.startSeed = startSeed;
        this.parquetFile = new File(parquetFile);
        this.schema = MessageTypeParser.parseMessageType(getSchemaMessage(topic, columnDefs));
        try {
            this.writer = ParquetWriter.writeFile(schema, this.parquetFile, createDehydrator());
        } catch (IOException ex) {
            throw new RuntimeException("Failed to create Parquet writer for topic: " + topic, ex);
        }
    }

    /**
     * Produce a maximum number of records asynchronously.
     *
     * @param perRecordPauseMillis wait time between each record sent
     * @param maxRecordCount maximum records to produce
     * @param maxDurationSecs maximum duration to produce
     */
    public Future<Metrics> produce(int perRecordPauseMillis, long maxRecordCount, int maxDurationSecs) {
        checkClosed();
        return queue.submit(() -> {
            final long maxDuration = maxDurationSecs * 1000L;
            final long beginTime = System.currentTimeMillis();
            final int colCount = columnDefs.getCount();

            long recCount = startSeed;
            long totalWritten = 0;
            long duration = 0;
            Object[] row = new Object[colCount];

            while (!isClosed.get() && recCount < maxRecordCount) {
                for (int i = 0; i < colCount; i++) {
                    row[i] = columnDefs.nextValue(i, recCount, maxRecordCount);
                }
                writer.write(row);
                recCount++;

                if (++totalWritten % 10_000_000 == 0)
                    Log.info("Produced %s records to topic: %s", totalWritten, topic);

                duration = System.currentTimeMillis() - beginTime;
                if (duration > maxDuration)
                    break;
            }

            Log.info("Produced %s records to topic: %s", totalWritten, topic);
            duration = System.currentTimeMillis() - beginTime;
            return new Metrics("test-runner", "generate." + topic)
                    .set("duration.secs", duration / 1000.0)
                    .set("record.count", totalWritten)
                    .set("send.rate", totalWritten / (duration / 1000.0));
        });
    }

    /**
     * Close the producer and shutdown any async threads created during production
     */
    public void close() {
        if (isClosed.get())
            return;
        isClosed.set(true);
        queue.shutdown();
        try {
            writer.close();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to close Parquet writer for topic: " + topic, ex);
        }
    }

    private void checkClosed() {
        if (isClosed.get())
            throw new RuntimeException("Generator is closed");
    }

    private Dehydrator<Object[]> createDehydrator() {
        final String[] colNames = columnDefs.toTypeMap().keySet().toArray(new String[0]);
        return (row, valueWriter) -> {
            for (int i = 0; i < colNames.length; i++) {
                valueWriter.write(colNames[i], row[i]);
            }
        };
    }

    private String getSchemaMessage(String topic, ColumnDefs fieldDefs) {
        var schema = """
        message ${topic} {
            ${fields}
        }
        """;
        var fields = "";
        for (Map.Entry<String, String> e : fieldDefs.toTypeMap().entrySet()) {
            var name = e.getKey();
            var type = e.getValue();
            fields += String.format("required %s %s %s;\n", getFieldType(type), name, getCharEncoding(type));
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
            case "string" -> "binary";
            case "timestamp-millis" -> "int64";
            default -> throw new RuntimeException("Unsupported generator data type: " + type);
        };
    }

    private String getCharEncoding(String type) {
        return switch (type) {
            case "string" -> "(UTF8)";
            case "timestamp-millis" -> "(TIMESTAMP(MILLIS,true))";
            default -> "";
        };
    }

}
