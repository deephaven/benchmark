package io.deephaven.benchmark.tests.internal.generator;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.api.Bench;

/**
 * Test to ensure that data types defined for the generator are propagated through Kafka to parquet and read according
 * to the correct corresponding Deephaven data types.
 */
public class DataTypesTest {
    final Bench api = Bench.create(this);

    @Test
    public void avroDataTypes() {
        api.table("datatypes").fixed()
                .add("col_timestamp_millis", "timestamp-millis", "[1676557157537-1676557157537]")
                .add("col_long", "long", "1")
                .add("col_int", "int", "2")
                .add("col_double", "double", "3")
                .add("col_float", "float", "4")
                .add("col_string", "string", "5")
                .generateParquet();

        var query = """
        from deephaven.parquet import read

        datatypes = read("/data/datatypes.parquet")
        meta = datatypes.meta_table
        """;

        api.query(query).fetchAfter("meta", table -> {
            assertEquals("io.deephaven.time.DateTime", table.getValue(0, "DataType").toString(), "Wrong data type");
            assertEquals("long", table.getValue(1, "DataType").toString(), "Wrong data type");
            assertEquals("int", table.getValue(2, "DataType").toString(), "Wrong data type");
            assertEquals("double", table.getValue(3, "DataType").toString(), "Wrong data type");
            assertEquals("float", table.getValue(4, "DataType").toString(), "Wrong data type");
            assertEquals("java.lang.String", table.getValue(5, "DataType").toString(), "Wrong data type");
        }).execute();
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
