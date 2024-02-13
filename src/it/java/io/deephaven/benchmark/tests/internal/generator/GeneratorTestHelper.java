/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.internal.generator;

import static org.junit.jupiter.api.Assertions.assertEquals;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.api.BenchTable;

class GeneratorTestHelper {

    static BenchTable makeTestTable(Bench api, String tableName) {
        return api.table(tableName).withDefaultDistribution("ascending")
        .add("col_timestamp_millis", "timestamp-millis", "[1676557157537-1676557157537]")
        .add("col_long", "long", "1")
        .add("col_int", "int", "2")
        .add("col_double", "double", "3")
        .add("col_float", "float", "4")
        .add("col_string", "string", "5")
        .withRowCount(10);
    }

    static void assertTestResults(Bench api, String query, String metaTableName, String tableName) {
        api.query(query).fetchAfter(metaTableName, table -> {
            // Start with 3, because Kafka prepends three columns to each message
            assertEquals("java.time.Instant", table.getValue(3, "DataType").toString(), "Wrong data type");
            assertEquals("long", table.getValue(4, "DataType").toString(), "Wrong data type");
            assertEquals("int", table.getValue(5, "DataType").toString(), "Wrong data type");
            assertEquals("double", table.getValue(6, "DataType").toString(), "Wrong data type");
            assertEquals("float", table.getValue(7, "DataType").toString(), "Wrong data type");
            assertEquals("java.lang.String", table.getValue(8, "DataType").toString(), "Wrong data type");
        }).fetchAfter(tableName, table -> {
            assertEquals("2023-02-16T14:19:17.537Z", table.getValue(0, "col_timestamp_millis").toString(),
                    "Wrong value");
            assertEquals(1L, table.getValue(0, "col_long"), "Wrong value");
            assertEquals(2, table.getValue(0, "col_int"), "Wrong value");
            assertEquals(3.0, table.getValue(0, "col_double"), "Wrong value");
            assertEquals(4.0f, table.getValue(0, "col_float"), "Wrong value");
            assertEquals("5", table.getValue(0, "col_string"), "Wrong value");
        }).execute();
    }

}
