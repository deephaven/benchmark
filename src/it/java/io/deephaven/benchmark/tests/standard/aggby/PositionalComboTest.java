/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.aggby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the agg_by table operation. Applies aggregations to table data base on in-group position
 */
public class PositionalComboTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    public void setup() {
        runner.setRowFactor(5);
        runner.tables("source");
        runner.addSetupQuery("from deephaven import agg");
    }

    @Test
    public void aggBy4FirstLastOps2Groups() {
        runner.setScaleFactors(3, 1);
        var aggs = """
        aggs = [
           agg.first(['int250']), agg.last(['int640']), 
           agg.first(['int1M']), agg.last(['str1M'])
        ]
        """;
        runner.addSetupQuery(aggs);

        var q = "source.agg_by(aggs, by=['str250', 'str640'])";
        runner.test("FirstLast-AggBy- 4 Ops 2 Groups 160K Unique Vals", 160000, q, "str640", "str250", "str1M",
                "int250", "int640", "int1M");
    }

}
