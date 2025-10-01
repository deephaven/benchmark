/* Copyright (c) 2025-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the countWhere table operation. Returns the number of rows for each group.
 */
public class CountWhereTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(6);
        runner.tables("source");

        var setupStr = """
        from deephaven import agg
        
        aggs=[agg.count_where(col="count", filters=["num1 > 3"])]
        """;
        runner.addSetupQuery(setupStr);
    }

    @Test
    void countWhere1Group() {
        runner.setScaleFactors(8, 7);
        var q = "source.agg_by(aggs, by=['key1'])";
        runner.test("CountWhere-AggBy- 1 Group 100 Unique Vals", 100, q, "key1", "num1");
    }

    @Test
    void countWhere2Group() {
        runner.setScaleFactors(3, 2);
        var q = "source.agg_by(aggs, by=['key1', 'key2'])";
        runner.test("CountWhere-AggBy- 2 Groups 10K Unique Combos", 10100, q, "key1", "key2", "num1");
    }

    @Test
    void countWhere3Group() {
        runner.setScaleFactors(2, 1);
        var q = "source.agg_by(aggs, by=['key1', 'key2', 'key3'])";
        runner.test("CountWhere-AggBy- 3 Groups 100K Unique Combos", 90900, q, "key1", "key2", "key3", "num1");
    }

}
