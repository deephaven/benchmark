/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.internal.join;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import io.deephaven.benchmark.api.Bench;

public class NaturalJoinAndJoin3TablesTest {
    final Bench api = Bench.create(this);
    private long scaleRowCount = api.propertyAsIntegral("scale.row.count", "10000");

    @BeforeEach
    public void setup() {
        api.table("animals")
                .add("animal_id", "int", "[1-250]")
                .add("animal_name", "string", "animal[1-250]")
                .withDefaultDistribution("ascending")
                .withFixedRowCount(true)                                               
                .generateParquet();

        api.table("adjectives")
                .add("adjective_id", "int", "[1-644]")
                .add("adjective_name", "string", "[1-644]")
                .withDefaultDistribution("ascending")
                .withFixedRowCount(true)
                .generateParquet();

        api.table("relation")
                .add("Values", "int", "[1-" + scaleRowCount + "]") // Values in bencher has 1 unique value per record
                .add("adjective_id", "int", "[1-644]") // Bencher allows random or incremental defined per column
                .add("animal_id", "int", "[1-250]") // Relation in bencher allows 5% nulls
                .withDefaultDistribution("ascending")
                .withFixedRowCount(true)
                .generateParquet();
    }

    @Test
    public void joinThreeTablesFromParquetViews() {
        api.setName("Join animals and adjectives - Parquet Views");

        var query = """
        from deephaven.parquet import read

        animals = read('/data/animals.parquet')
        adjectives = read('/data/adjectives.parquet')
        relation = read('/data/relation.parquet')

        result = relation.natural_join(adjectives, on=['adjective_id']).join(animals, on=['animal_id']).view(formulas=['Values', 'adjective_name', 'animal_name'])

        from deephaven.column import int_col
        result_row_count = new_table([int_col("result_size", [result.size])])
        """;

        api.query(query).fetchAfter("result_row_count", table -> {
            int recCount = table.getSum("result_size").intValue();
            assertEquals(scaleRowCount, recCount, "Wrong record count");
        }).execute();
    }

    @Test
    public void joinThreeTablesFromParquetFileIncRelease() {
        api.setName("Join animals and adjectives - Incremental Release");

        var query = """
        from deephaven.parquet import read

        animals = read('/data/animals.parquet')
        adjectives = read('/data/adjectives.parquet')
        relation = read('/data/relation.parquet').select(formulas=['adjective_id', 'animal_id', 'Values'])

        autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
        relation_filter = autotune(0, 1000000, 1.0, True)
        relation = relation.where(relation_filter)

        result = relation.natural_join(adjectives, on=['adjective_id']).join(animals, on=['animal_id']).view(formulas=['Values', 'adjective_name', 'animal_name'])

        relation_filter.start()
        relation_filter.waitForCompletion()

        from deephaven.column import int_col
        result_row_count = new_table([int_col("result_size", [result.size])])
        """;

        api.query(query).fetchAfter("result_row_count", table -> {
            int recCount = table.getSum("result_size").intValue();
            assertEquals(scaleRowCount, recCount, "Wrong record count");
        }).execute();
    }

    @AfterEach
    public void teardown() {
        api.close();
    }

}
