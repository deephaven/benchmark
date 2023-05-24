/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.kafka;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Timer;

class KafkaTestRunner {
    final Object testInst;
    final Bench api;
    private long rowCount;
    private int colCount;
    private String colType;
    private String generatorType;

    KafkaTestRunner(Object testInst) {
        this.testInst = testInst;
        this.api = Bench.create(testInst);
    }

    void restartWithHeap(int deephavenHeapGigs) {
        restartDocker(api, deephavenHeapGigs);
    }

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
        if (generatorType.equals("json")) {
            table.withRowCount(rowCount).generateJson();
        } else if (generatorType.equals("avro")) {
            table.withRowCount(rowCount).generateAvro();
        } else {
            throw new RuntimeException("Bad generator type: " + generatorType);
        }
        api.awaitCompletion();
    }

    void runTest(String operation, String tableType) {
        var query = """
        import time
        from deephaven import new_table, garbage_collect
        from deephaven.column import long_col, double_col
        from deephaven.table import Table
        from deephaven.ugp import exclusive_lock
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
        import deephaven.dtypes as dht
        
        kc_spec = ${kafkaConsumerSpec}
        
        consumer_tbl = kc.consume({ 'bootstrap.servers' : '${kafka.consumer.addr}' ${schemaRegistryURL} },
            'consumer_tbl',
            offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE,
            value_spec=kc_spec,
            table_type=TableType.${tableType}())
              
        bench_api_metrics_snapshot()
        begin_time = time.perf_counter_ns()
        result = ${operation}
        
        bench_api_await_column_value_limit(result, 'count', ${rowCount})
        
        end_time = time.perf_counter_ns()
        bench_api_metrics_snapshot()
        standard_metrics = bench_api_metrics_collect()
        
        stats = new_table([
            double_col("elapsed_nanos", [end_time - begin_time]),
            long_col("processed_row_count", [result.j_object.getColumnSource('count').get(0)]),
            long_col("result_row_count", [result.size]),
        ])
        """;
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
    }

    String getSchemaRegistry() {
        if (!generatorType.equals("avro")) {
            return "";
        }
        return ", 'schema.registry.url' : 'http://${schema.registry.addr}'";
    }

    String getKafkaConsumerSpec(int colCount, String colType) {
        if (generatorType.equals("avro")) {
            return "kc.avro_spec('consumer_tbl_record', schema_version='1')";
        }
        var spec = """
        [('count', dht.long)]
        for i in range(1, ${colCount}):
            kc_spec.append(('col' + str(i + 1), dht.${colType}))
        kc_spec = kc.json_spec(kc_spec)
        """;
        return spec.replace("${colCount}", "" + colCount).replace("${colType}", "" + colType);
    }

    void restartDocker(Bench api, int heapGigs) {
        String dockerComposeFile = api.property("docker.compose.file", "");
        if (dockerComposeFile.isBlank())
            return;
        dockerComposeFile = makeHeapAdjustedDockerCompose(dockerComposeFile, heapGigs);
        var timer = api.timer();
        Exec.restartDocker(dockerComposeFile);
        var metrics = new Metrics(Timer.now(), "test-runner", "setup", "docker");
        metrics.set("restart", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    // Replace heap (e.g. -Xmx64g) in docker-compose.yml with new heap value
    String makeHeapAdjustedDockerCompose(String dockerComposeFile, int heapGigs) {
        Path sourceComposeFile = Paths.get(dockerComposeFile);
        String newComposeName = sourceComposeFile.getFileName().toString().replace(".yml", "." + heapGigs + "g.yml");
        Path destComposeFile = sourceComposeFile.resolveSibling(newComposeName);
        String composeText = Filer.getFileText(sourceComposeFile);
        composeText = composeText.replaceAll("[-]Xmx[0-9]+[gG]", "-Xmx" + heapGigs + "g");
        Filer.putFileText(destComposeFile, composeText);
        return destComposeFile.toString();
    }

    String getDHType(String genColType) {
        switch (genColType) {
            case "int":
                return "int32";
            case "timestamp-millis":
                return "DateTime";
            default:
                return genColType;
        }
    }

}
