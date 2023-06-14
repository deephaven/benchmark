package io.deephaven.benchmark.tests.internal.generator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test writing to Kafka and consuming using Avro formatting and specs.
 */
public class AvroGeneratorTest {
    final Bench api = Bench.create(this);

    @Test
    public void avroGenerator() {
        api.table("avro_generated").fixed()
                .add("col_timestamp_millis", "timestamp-millis", "[1676557157537-1676557157537]")
                .add("col_long", "long", "1")
                .add("col_int", "int", "2")
                .add("col_double", "double", "3")
                .add("col_float", "float", "4")
                .add("col_string", "string", "5")
                .withRowCount(10)
                .generateAvro();

        var query = """
        import time
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec

        avro_generated = kc.consume(
            { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
            'avro_generated', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('avro_generated_record', schema_version='1'),
            table_type=TableType.append())
        
        time.sleep(1)
        meta = avro_generated.meta_table
        
        """;

        api.query(query).fetchAfter("meta", table -> {
            // Start with 3, because Kafka prepends three columns to each message
            assertEquals("java.time.Instant", table.getValue(3, "DataType").toString(), "Wrong data type");
            assertEquals("long", table.getValue(4, "DataType").toString(), "Wrong data type");
            assertEquals("int", table.getValue(5, "DataType").toString(), "Wrong data type");
            assertEquals("double", table.getValue(6, "DataType").toString(), "Wrong data type");
            assertEquals("float", table.getValue(7, "DataType").toString(), "Wrong data type");
            assertEquals("java.lang.String", table.getValue(8, "DataType").toString(), "Wrong data type");
        }).fetchAfter("avro_generated", table -> {
            assertEquals("2023-02-16T14:19:17.537Z", table.getValue(0, "col_timestamp_millis").toString(),
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
