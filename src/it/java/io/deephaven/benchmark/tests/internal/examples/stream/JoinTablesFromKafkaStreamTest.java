/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.internal.examples.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Join two tables that are generated from a Kafka stream. Demonstrates how to configure the tables, produce data to
 * Kafka topics, consume the topics to Deephaven tables, and query the tables. (Note: The join here is deliberately
 * inefficient to show the difference between generation rate and query processing rate.)
 */
public class JoinTablesFromKafkaStreamTest {
    final Bench api = Bench.create(this);
    final long scaleRowCount = api.propertyAsIntegral("scale.row.count", "1000");

    @BeforeEach
    public void setup() {
        api.table("stock_info").fixed()
                .add("symbol", "string", "SYM[1-10000]")
                .add("description", "string", "ABC[1-10000] CORP")
                .add("exchange", "string", "EXCHANGE[1-10]")
                .generateAvro();

        api.awaitCompletion();

        api.table("stock_trans")
                .add("symbol", "string", "SYM[1-10000]")
                .add("price", "float", "[100-200]")
                .add("buys", "int", "[1-100]")
                .add("sells", "int", "[1-100]")
                .generateAvro();
    }

    /**
     * Join two tables from a Kafka stream generated by the test where one table is fixed size and the other is scaled:
     * <ol>
     * <li>Generate data to two kafka topics; stock_info and stock_trans</li>
     * <li>Create two corresponding tables in Deephaven Engine with the Kafka consumer
     * <li>Join the two tables and do some aggregations</li>
     * <li>End the query when all data has been consumed and aggregated</li>
     * </ol>
     * This test is identical to {@link #joinTwoTablesFromKafkaStream_Shorthand} except without the use of
     * <code>bench_api_</code> functions for Kafka consumers and table waiting.
     * <p/>
     * Properties (e.g. ${kafka.consumer.addr}) are automatically filled in during query execution.
     */
    @Test
    public void joinTwoTablesFromKafkaStream_Longhand() {
        api.setName("Join Two Tables Using Kakfa Streams - Longhand Query");

        var query = """
        from deephaven import agg
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
        from deephaven.table import Table
        from deephaven.update_graph import exclusive_lock

        kafka_stock_info = kc.consume(
            { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
            'stock_info', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('stock_info_record', schema_version='1'),
            table_type=TableType.append())

        kafka_stock_trans = kc.consume(
            { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
            'stock_trans', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('stock_trans_record', schema_version='1'),
            table_type=TableType.append())

        stock_info = kafka_stock_info.view(formulas=['symbol', 'description', 'exchange'])
        stock_trans = kafka_stock_trans.view(formulas=['symbol', 'timestamp=KafkaTimestamp', 'price', 'buys', 'sells', 'rec_count=1'])

        aggs = [
            agg.avg('AvgPrice=price'), agg.min_('LowPrice=price'), agg.max_('HighPrice=price'),
            agg.sum_('Buys=buys'), agg.sum_('Sells=sells'), agg.sum_('RecordCount=rec_count')
        ]

        by = ['symbol', 'description', 'exchange']

        formulas = [
            'Symbol=symbol', 'Description=description', 'Exchange=exchange', 'AvgPrice',
            'LowPrice', 'HighPrice', 'Volume=Buys+Sells', 'RecordCount'
        ]

        stock_volume = stock_trans.join(stock_info, on=['symbol']).agg_by(aggs, by).view(formulas)
        stock_exchange = stock_volume.agg_by([agg.sum_('Volume'), agg.sum_('RecordCount')], by=['Exchange'])
        record_count = stock_exchange.agg_by([agg.sum_('RecordCount')])

        def await_table_size(table: Table, row_count: int):
            with exclusive_lock(table):
                while table.j_table.size() < row_count:
                    table.j_table.awaitUpdate()

        await_table_size(kafka_stock_trans, ${scale.row.count})
        """;

        var tm = api.timer();
        api.query(query).fetchAfter("record_count", table -> { // Look at using Barrage tables
            int recCount = table.getSum("RecordCount").intValue();
            assertEquals(scaleRowCount, recCount, "Wrong record count");
        }).execute();
        api.awaitCompletion();
        api.result().test("deephaven-engine", tm, scaleRowCount);
    }

    /**
     * Join two tables from a Kafka stream generated by the test where one table is fixed size and the other is scaled:
     * <ol>
     * <li>Generate data to two kafka topics; stock_info and stock_trans</li>
     * <li>Create two corresponding tables in Deephaven Engine with the Kafka consumer
     * <li>Join the two tables and do some aggregations</li>
     * <li>End the query when all data has been consumed and aggregated</li>
     * </ol>
     * This test is identical to {@link #joinTwoTablesFromKafkaStream_Longhand} except without the use of
     * <code>bench_api_</code> functions for Kafka consumers and table waiting.
     * <p/>
     * Properties (e.g. ${kafka.consumer.addr}) are automatically filled in during query execution.
     */
    @Test
    public void joinTwoTablesFromKafkaStream_Shorthand() {
        api.setName("Join Two Tables Using Kakfa Streams - Shorthand Query");

        var query = """
        from deephaven import agg

        kafka_stock_info = bench_api_kafka_consume('stock_info', 'append')
        kafka_stock_trans = bench_api_kafka_consume('stock_trans', 'append')

        stock_info = kafka_stock_info.view(formulas=['symbol', 'description', 'exchange'])
        stock_trans = kafka_stock_trans.view(formulas=['symbol', 'timestamp=KafkaTimestamp', 'price', 'buys', 'sells', 'rec_count=1'])

        aggs = [
            agg.avg('AvgPrice=price'), agg.min_('LowPrice=price'), agg.max_('HighPrice=price'),
            agg.sum_('Buys=buys'), agg.sum_('Sells=sells'), agg.sum_('RecordCount=rec_count')
        ]

        by = ['symbol', 'description', 'exchange']

        formulas = [
            'Symbol=symbol', 'Description=description', 'Exchange=exchange', 'AvgPrice',
            'LowPrice', 'HighPrice', 'Volume=Buys+Sells', 'RecordCount'
        ]

        stock_volume = stock_trans.join(stock_info, on=['symbol']).agg_by(aggs, by).view(formulas)
        stock_exchange = stock_volume.agg_by([agg.sum_('Volume'), agg.sum_('RecordCount')], by=['Exchange'])
        record_count = stock_exchange.agg_by([agg.sum_('RecordCount')])

        bench_api_await_table_size(kafka_stock_trans, ${scale.row.count})
        """;

        var tm = api.timer();
        api.query(query).fetchAfter("record_count", table -> {
            int recCount = table.getSum("RecordCount").intValue();
            assertEquals(scaleRowCount, recCount, "Wrong record count");
        }).execute();
        api.awaitCompletion();
        api.result().test("deephaven-engine", tm, scaleRowCount);
    }

    /**
     * Count the records from a Kafka topic as the stream is being produced. Nothing is done with the data other than
     * counting the rows. This shows the overhead of only reading the Kafka topic into Deephaven without any other
     * operations:
     * <ol>
     * <li>Generate data to one kafka topic; stock_trans</li>
     * <li>Create one corresponding table in Deephaven Engine with the Kafka consumer
     * <li>End the query when all data has been consumed</li>
     * </ol>
     * Properties (e.g. ${scale.row.count}) are automatically filled in during query execution.
     */
    @Test
    public void countRecordsFromKafkaStream() {
        api.setName("Count Records From Kakfa Stream");

        var query = """
        kafka_stock_trans = bench_api_kafka_consume('stock_trans', 'append')
        bench_api_await_table_size(kafka_stock_trans, ${scale.row.count})
        """;

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
