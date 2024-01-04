package io.deephaven.benchmark.tests.internal.generator;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test writing to Kafka and consuming using json formatting and specs.
 */
public class JsonGeneratorTest {
    final Bench api = Bench.create(this);

    @Test
    public void jsonGenerator() {
        GeneratorTestHelper.makeTestTable(api, "json_generated").generateJson();

        var query = """
        import time
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
        import deephaven.dtypes as dht

        json_spec = kc.json_spec([
            ('col_timestamp_millis', dht.Instant),
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

        GeneratorTestHelper.assertTestResults(api, query, "meta", "json_generated");
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
