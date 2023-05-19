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
    final long scaleRowCount = 2_000_000;  // 21474836480L;

    @BeforeEach
    public void setup() {
        var beginTime = System.currentTimeMillis();
        var table = api.table("consumer_tbl").fixed();
        table.add("count", "long", "[1-" + scaleRowCount + "]");
        for(int i = 0; i < 100; i++) {
            table.add("col" + i, "double", "[1-1000]");
        }
        table.withRowCount(scaleRowCount).generateJson();

        api.awaitCompletion();
        var endTime = System.currentTimeMillis();
        System.out.println("KafkaHeapTest Generation Time: " + (1.0 * scaleRowCount / (endTime - beginTime) * 1000.0) + " rows/sec");
    }
    
  
//  @Test
//  public void CountByFromKafkaAvroStream() {
//      api.setName("CountBy- Kafka Consumer Avro Stream");
//
//      var query = """
//      from deephaven.table import Table
//      from deephaven.ugp import exclusive_lock
//
//      consumer_tbl = bench_api_kafka_consume('consumer_tbl', 'stream')
//      row_count = consumer_tbl.count_by("count")
//      
//      def bench_api_await_row_count(table: Table, row_count: int):
//          with exclusive_lock():
//              count = 0
//              while count < row_count:
//                  table.j_table.awaitUpdate()
//                  count = table.j_object.getColumnSource("count").get(0)
//
//      bench_api_await_row_count(row_count, ${scaleRowCount})
//      """.replace("${scaleRowCount}", "" + scaleRowCount);
//
//      var tm = api.timer();
//      api.query(query).execute();
//      api.awaitCompletion();
//      api.result().test("deephaven-engine", tm, scaleRowCount);
//  }

    
//    @Test
//    public void CountByFromKafkaAvroAppend() {
//        api.setName("CountBy- Kafka Consumer Avro Append");
//
//        var query = """
//        from deephaven.table import Table
//        from deephaven.ugp import exclusive_lock
//
//        consumer_tbl = bench_api_kafka_consume('consumer_tbl', 'append')
//        row_count = consumer_tbl.count_by("count")
//        
//        def bench_api_await_row_count(table: Table, row_count: int):
//            with exclusive_lock():
//                count = 0
//                while count < row_count:
//                    table.j_table.awaitUpdate()
//                    count = table.j_object.getColumnSource("count").get(0)
//
//        bench_api_await_row_count(row_count, ${scaleRowCount})
//        """.replace("${scaleRowCount}", "" + scaleRowCount);
//
//        var tm = api.timer();
//        api.query(query).execute();
//        api.awaitCompletion();
//        api.result().test("deephaven-engine", tm, scaleRowCount);
//    }

    @Test
    public void CountByFromKafkaJsonStream() {
        api.setName("CountBy- Kafka Consumer Json Stream");
        

//        var query = """
//        from deephaven.table import Table
//        from deephaven.ugp import exclusive_lock
//        from deephaven import kafka_consumer as kc
//        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
//        import deephaven.dtypes as dht
//
//        consumer_tbl = kc.consume({ 'bootstrap.servers' : 'redpanda:29092' },
//            'consumer_tbl',
//            offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
//            key_spec=KeyValueSpec.IGNORE,
//            value_spec=kc.json_spec([('count', dht.long),
//                 ('col2', dht.long),
//                 ('col3', dht.long),
//                 ('col4', dht.long)]),
//            table_type=TableType.stream())
//            
//        row_count = consumer_tbl.count_by("count")
//        
//        def bench_api_await_row_count(table: Table, row_count: int):
//            with exclusive_lock():
//                count = 0
//                while count < row_count:
//                    table.j_table.awaitUpdate()
//                    count = table.j_object.getColumnSource("count").get(0)
//
//        bench_api_await_row_count(row_count, ${scaleRowCount})
//        """.replace("${scaleRowCount}", "" + scaleRowCount);
//
//        var tm = api.timer();
//        api.query(query).execute();
//        api.awaitCompletion();
//        api.result().test("deephaven-engine", tm, scaleRowCount);
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
