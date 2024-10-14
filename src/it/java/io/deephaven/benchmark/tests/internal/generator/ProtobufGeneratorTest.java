package io.deephaven.benchmark.tests.internal.generator;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test writing to Kafka and consuming using Protobuf formatting and specs.
 */
public class ProtobufGeneratorTest {
    final Bench api = Bench.create(this);

    @Test
    public void protobufGenerator() {
        GeneratorTestHelper.makeTestTable(api, "protobuf_generated").generateProtobuf();

        var query = """
        import time
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec

        protobuf_generated = kc.consume(
            { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
            'protobuf_generated', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
            key_spec=KeyValueSpec.IGNORE, value_spec=kc.protobuf_spec('protobuf_generated_record', schema_version=1),
            table_type=TableType.append())
        
        time.sleep(1)
        meta = protobuf_generated.meta_table
        
        """;

        GeneratorTestHelper.assertTestResults(api, query, "meta", "protobuf_generated");
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
