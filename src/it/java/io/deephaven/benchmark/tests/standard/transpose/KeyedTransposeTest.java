/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.transpose;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the aggBy table operation. Applies basic math aggregations to table data
 */
@Disabled
public class KeyedTransposeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(1);
        runner.tables("source");

        var setupStr = """
        from deephaven import agg
        from deephaven.table import keyed_transpose, NewColumnBehaviorType
        
        aggs = [agg.count_('Count'), agg.sum_('Sum=num1')]
       
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void keyedTranspose2Agg2RowBy1ColBy() {
        runner.setScaleFactors(0, 1);
        var setup = """
        init_groups = new_table([int_col('key3', [0, -1, 2, -3, 4, -5, 6, -7, 8])]).join(
            source.select_distinct(['key1', 'key2']))
        """;
        runner.addPreOpQuery(setup);
        var q = "keyed_transpose(source, aggs, ['key1', 'key2'], ['key3'], None, NewColumnBehaviorType.IGNORE)";
        runner.test("KeyedTranspose- 2 Aggs 2 RowBy 1 ColBy", 10100, q, "key1", "key2", "key3", "num1");
    }

}
