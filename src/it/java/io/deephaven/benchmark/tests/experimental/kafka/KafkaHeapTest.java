package io.deephaven.benchmark.tests.experimental.kafka;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.deephaven.benchmark.api.Bench;

/**
 * Generate data to a Kafka topic using low memory for both Deephaven and Redpanda, then consume the data from the topic
 * in various ways.
 */
public class KafkaHeapTest {
    final Bench api = Bench.create(this);
    final long scaleRowCount = 100_000_000;  // 21474836480L;

    @BeforeEach
    public void setup() {
        var beginTime = System.currentTimeMillis();
        api.table("int_tbl").fixed()
                .add("count", "long", "[1-" + scaleRowCount + "]")
                .add("col2", "long", "[1-10]")
                .add("col3", "long", "[1-100]")
                .add("col4", "long", "[1-1000]")
                .withRowCount(scaleRowCount)
                .generateAvro();

        api.awaitCompletion();
        var endTime = System.currentTimeMillis();
        System.out.println("KafkaHeapTest Generation Time: " + (1.0 * scaleRowCount / (endTime - beginTime) * 1000.0) + " rows/sec");
    }
    
    @Test
    public void LastByFromKafkaStream() {
        api.setName("LastBy- Kafka Consumer Stream");

        var query = """
        from deephaven.table import Table
        from deephaven.ugp import exclusive_lock

        int_tbl = bench_api_kafka_consume('int_tbl', 'stream')
        row_count = int_tbl.last_by()
        
        def bench_api_await_row_count(table: Table, row_count: int):
            with exclusive_lock():
                count = 0
                while count < row_count:
                    table.j_table.awaitUpdate()
                    count = table.j_object.getColumnSource("count").get(0)

        bench_api_await_row_count(row_count, ${scaleRowCount})
        """.replace("${scaleRowCount}", "" + scaleRowCount);

        var tm = api.timer();
        api.query(query).execute();
        api.awaitCompletion();
        api.result().test("deephaven-engine", tm, scaleRowCount);
    }
    
    @Test
    public void CountByFromKafkaStream() {
        api.setName("CountBy- Kafka Consumer Stream");

        var query = """
        from deephaven.table import Table
        from deephaven.ugp import exclusive_lock

        int_tbl = bench_api_kafka_consume('int_tbl', 'stream')
        row_count = int_tbl.count_by("count")
        
        def bench_api_await_row_count(table: Table, row_count: int):
            with exclusive_lock():
                count = 0
                while count < row_count:
                    table.j_table.awaitUpdate()
                    count = table.j_object.getColumnSource("count").get(0)

        bench_api_await_row_count(row_count, ${scaleRowCount})
        """.replace("${scaleRowCount}", "" + scaleRowCount);

        var tm = api.timer();
        api.query(query).execute();
        api.awaitCompletion();
        api.result().test("deephaven-engine", tm, scaleRowCount);
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
