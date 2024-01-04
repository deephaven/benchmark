package io.deephaven.benchmark.tests.internal.generator;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test writing to Kafka and consuming using Avro formatting and specs.
 */
public class AvroGeneratorTest {
    final Bench api = Bench.create(this);

    @Test
    public void avroGenerator() {
        GeneratorTestHelper.makeTestTable(api, "avro_generated").generateAvro();

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

        GeneratorTestHelper.assertTestResults(api, query, "meta", "avro_generated");  
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
