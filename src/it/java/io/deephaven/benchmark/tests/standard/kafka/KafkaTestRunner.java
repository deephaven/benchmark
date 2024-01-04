/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.controller.Controller;
import io.deephaven.benchmark.controller.DeephavenDockerController;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Timer;

/**
 * Used exclusively as a Bench API wrapper to run the Kafka tests. Provides generation of test data through Kafka,
 * running tests for Kafka consumer according to column data types, JSON/Avro/Protobuf, wide/narrow tables, and
 * append/blink table types. Results are checked to ensure the correct number of rows has been processed.
 */
class KafkaTestRunner {
    final Object testInst;
    final Bench api;
    final Controller controller;
    private long rowCount;
    private int colCount;
    private String colType;
    private String generatorType;

    /**
     * Initialize the test runner and the Bench API
     * 
     * @param testInst the test instance used by the Bench API
     */
    KafkaTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = Bench.create(testInst);
        this.controller = new DeephavenDockerController(api.property("docker.compose.file", ""),
                api.property("deephaven.addr", ""));
    }

    /**
     * If a {@code docker.compose.file} is specified in supplied runtime properties, restart the corresponding docker
     * images with Deephaven max heap set to the given gigabytes.
     * 
     * @param deephavenHeapGigs the number of gigabytes to use for Deephave max heap
     */
    void restartWithHeap(int deephavenHeapGigs) {
        String dockerComposeFile = api.property("docker.compose.file", "");
        String deephavenHostPort = api.property("deephaven.addr", "");
        if (dockerComposeFile.isBlank() || deephavenHostPort.isBlank())
            return;
        dockerComposeFile = makeHeapAdjustedDockerCompose(dockerComposeFile, deephavenHeapGigs);
        var timer = api.timer();
        controller.restartService();
        var metrics = new Metrics(Timer.now(), "test-runner", "setup", "docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    /**
     * Generate the consumer table to be used for the test.
     * 
     * @param rowCount the number of rows to generate
     * @param colCount the number of columns to generate for each row
     * @param colType the type of column to generate and consume
     * @param generatorType the format in which to generate the rows (e.g. avro | json | protobuf)
     */
    void table(long rowCount, int colCount, String colType, String generatorType) {
        this.rowCount = rowCount;
        this.colCount = colCount;
        this.colType = colType;
        this.generatorType = generatorType;

        var table = api.table("consumer_tbl").fixed();
        table.add("count", "long", "[1-" + rowCount + "]");
        for (int i = 1; i < colCount; i++) {
            table.add("col" + (i + 1), colType, "[1-1000]");
        }
        switch (generatorType) {
            case "json" -> table.withRowCount(rowCount).generateJson();
            case "avro" -> table.withRowCount(rowCount).generateAvro();
            case "protobuf" -> table.withRowCount(rowCount).generateProtobuf();
            default -> throw new RuntimeException("Bad generator type: " + generatorType);
        }
        api.awaitCompletion();
    }

    /**
     * Run the test and measure the rate of Kafka consumption. Note: Mixing 'None' operation with 'blink' table type is
     * undefined.
     * 
     * @param operation a Deephave operation or 'None'
     * @param tableType the Kafka consumer {@code TableType} (e.g. append | blink)
     */
    void runTest(String operation, String tableType) {
        var query = """
        import time
        from deephaven import new_table, garbage_collect
        from deephaven.column import long_col, double_col
        from deephaven.table import Table
        from deephaven.update_graph import exclusive_lock
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
        import deephaven.dtypes as dht
        
        kc_spec = ${kafkaConsumerSpec}
        
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        
        consumer_tbl = kc.consume({ 'bootstrap.servers' : '${kafka.consumer.addr}' ${schemaRegistryURL} },
            'consumer_tbl',
            offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE,
            value_spec=kc_spec,
            table_type=TableType.${tableType}())
        
        result = ${operation}
        ${awaitTableLoad}
        
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [${consumerResultSize}]),
            long_col("result_row_count", [${resultTableSize}])
        ])
        """;
        query = query.replace("${awaitTableLoad}", getTableWaiter(operation));
        query = query.replace("${consumerResultSize}", getConsumerResultSize(operation));
        query = query.replace("${resultTableSize}", getResultTableSize(operation));
        query = query.replace("${rowCount}", "" + rowCount);
        query = query.replace("${tableType}", tableType);
        query = query.replace("${operation}", operation);
        query = query.replace("${kafkaConsumerSpec}", getKafkaConsumerSpec(colCount, getDHType(colType)));
        query = query.replace("${schemaRegistryURL}", getSchemaRegistry());

        api.query(query).fetchAfter("stats", table -> {
            long elapsedNanos = table.getSum("elapsed_nanos").longValue();
            long procRowCount = table.getSum("processed_row_count").longValue();
            long resultRowCount = table.getSum("result_row_count").longValue();
            assertEquals(rowCount, procRowCount, "Wrong processed row count");
            assertEquals(1, resultRowCount, "Wrong counter table row count");
            api.result().test("deephaven-engine", Duration.ofNanos(elapsedNanos), rowCount);
        }).fetchAfter("standard_metrics", table -> {
            api.metrics().add(table);
        }).execute();
        addDockerLog(api);
    }

    private String getSchemaRegistry() {
        if (generatorType.equals("avro") || generatorType.equals("protobuf"))
            return ", 'schema.registry.url' : 'http://${schema.registry.addr}'";
        return "";
    }

    private String getKafkaConsumerSpec(int colCount, String colType) {
        if (generatorType.equals("avro")) {
            return "kc.avro_spec('consumer_tbl_record', schema_version='1')";
        }
        if (generatorType.equals("protobuf")) {
            return "kc.protobuf_spec('consumer_tbl_record', schema_version=1)";
        }
        var spec = """
        [('count', dht.long)]
        for i in range(1, ${colCount}):
            kc_spec.append(('col' + str(i + 1), dht.${colType}))
        kc_spec = kc.json_spec(kc_spec)
        """;
        return spec.replace("${colCount}", "" + colCount).replace("${colType}", "" + colType);
    }

    private String getTableWaiter(String operation) {
        if (operation.equals("None")) {
            return "bench_api_await_table_size(consumer_tbl, ${rowCount})";
        }
        return "bench_api_await_column_value_limit(result, 'count', ${rowCount})";
    }

    private String getConsumerResultSize(String operation) {
        if (operation.equals("None")) {
            return "consumer_tbl.size";
        }
        return "result.j_object.getColumnSource('count').get(0)";
    }

    private String getResultTableSize(String operation) {
        if (operation.equals("None")) {
            return "1";
        }
        return "result.size";
    }
    
    private void addDockerLog(Bench api) {
        var timer = api.timer();
        var logText = controller.getLog();
        if (logText.isBlank())
            return;
        api.log().add("deephaven-engine", logText);
        var metrics = new Metrics(Timer.now(), "test-runner", "teardown", "docker");
        metrics.set("log", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    // Replace heap (e.g. -Xmx64g) in docker-compose.yml with new heap value
    private String makeHeapAdjustedDockerCompose(String dockerComposeFile, int heapGigs) {
        Path sourceComposeFile = Paths.get(dockerComposeFile);
        String newComposeName = sourceComposeFile.getFileName().toString().replace(".yml", "." + heapGigs + "g.yml");
        Path destComposeFile = sourceComposeFile.resolveSibling(newComposeName);
        String composeText = Filer.getFileText(sourceComposeFile);
        composeText = composeText.replaceAll("[-]Xmx[0-9]+[gG]", "-Xmx" + heapGigs + "g");
        Filer.putFileText(destComposeFile, composeText);
        return destComposeFile.toString();
    }

    private String getDHType(String genColType) {
        switch (genColType) {
            case "int":
                return "int32";
            case "timestamp-millis":
                return "Instant";
            default:
                return genColType;
        }
    }

}
