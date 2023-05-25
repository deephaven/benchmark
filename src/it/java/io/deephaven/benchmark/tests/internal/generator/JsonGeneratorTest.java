package io.deephaven.benchmark.tests.internal.generator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test writing to Kafka and consuming using json formatting and specs.
 */
public class JsonGeneratorTest {
    final Bench api = Bench.create(this);

    @Test
    public void jsonGenerator() {
        api.table("json_generated").fixed()
                .add("col_timestamp_millis", "timestamp-millis", "[1676557157537-1676557157537]")
                .add("col_long", "long", "1")
                .add("col_int", "int", "2")
                .add("col_double", "double", "3")
                .add("col_float", "float", "4")
                .add("col_string", "string", "5")
                .withRowCount(10)
                .generateJson();

        var query = """
        import time
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
        import deephaven.dtypes as dht

        json_spec = kc.json_spec([
            ('col_timestamp_millis', dht.DateTime),
            ('col_long', dht.long),
            ('col_int', dht.int32),
            ('col_double', dht.double),
            ('col_float', dht.float32),
            ('col_string', dht.string)
        ])

        json_generated = kc.consume(
            { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
            'json_generated', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE, value_spec=json_spec,
            table_type=TableType.append())
        
        time.sleep(1)
        meta = json_generated.meta_table
        """;

        api.query(query).fetchAfter("meta", table -> {
            // Start with 3, because Kafka prepends three columns to each message
            assertEquals("io.deephaven.time.DateTime", table.getValue(3, "DataType").toString(), "Wrong data type");
            assertEquals("long", table.getValue(4, "DataType").toString(), "Wrong data type");
            assertEquals("int", table.getValue(5, "DataType").toString(), "Wrong data type");
            assertEquals("double", table.getValue(6, "DataType").toString(), "Wrong data type");
            assertEquals("float", table.getValue(7, "DataType").toString(), "Wrong data type");
            assertEquals("java.lang.String", table.getValue(8, "DataType").toString(), "Wrong data type");
        }).fetchAfter("json_generated", table -> {
            assertEquals("2023-02-16T09:19:17.537000000 NY", table.getValue(0, "col_timestamp_millis").toString(),
                    "Wrong value");
            assertEquals(1L, table.getValue(0, "col_long"), "Wrong value");
            assertEquals(2, table.getValue(0, "col_int"), "Wrong value");
            assertEquals(3.0, table.getValue(0, "col_double"), "Wrong value");
            assertEquals(4.0f, table.getValue(0, "col_float"), "Wrong value");
            assertEquals("5", table.getValue(0, "col_string"), "Wrong value");
        }).execute();
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
