/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.index;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for using the <code>data_index</code> and assessing its performance cost
 */
@TestMethodOrder(OrderAnnotation.class)
public class DataIndexLoadTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    void setup(int rowFactor, int staticFactor, int incFactor) {
        runner.setRowFactor(rowFactor);
        runner.tables("source");
        runner.setScaleFactors(staticFactor, incFactor);

        var setup = """
        from deephaven.experimental.data_index import data_index
        QueryTable = jpy.get_type('io.deephaven.engine.table.impl.QueryTable')
        QueryTable.setMemoizeResults(False)
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    @Order(1)
    void dataIndexLoadStatic() {
        setup(1, 3, 0);

        var op = """
        source_idx = data_index(source, ['key1','key2','key4'])
        result = source_idx.table
        QueryTable.setMemoizeResults(True)
        """;
        runner.test("DataIndex- Index Load 1M Unique Combos", 999900, op, "num1", "key1", "key2", "key4");
    }

    @Test
    @Order(2)
    void dataIndexLoadInc() {
        setup(1, 0, 2);

        var setup = """
        source_idx = data_index(source, ['key1','key2','key4'])
        source_idx.table
        """;
        runner.addPreOpQuery(setup);

        var op = """
        result = source     # Nothing to do to the source
        QueryTable.setMemoizeResults(True)
        """;
        runner.test("DataIndex- Index Load 1M Unique Combos", op, "num1", "key1", "key2", "key4");
    }

}
