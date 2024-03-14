/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling product. The result table contains
 * additional columns with windowed rolling products for each specified column in the source table. *
 * <p/>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingProdTickTest</code>
 */
public class RollingProdTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.setRowFactor(3);
        runner.tables("timed");

        var setup = """
        from deephaven.updateby import rolling_prod_time
        contains_row = rolling_prod_time(ts_col="timestamp", cols=["X=num1"], rev_time="PT4S", fwd_time="PT3S")
        before_row = rolling_prod_time(ts_col="timestamp", cols=["Y=num1"], rev_time="PT3S", fwd_time=int(-1e9))
        after_row = rolling_prod_time(ts_col="timestamp", cols=["Z=num1"], rev_time="-PT1S", fwd_time=int(3e9)) 
        """;
        runner.addSetupQuery(setup);
    }

    @Test
    void rollingProdTime0Group3Ops() {
        runner.setScaleFactors(3, 2);
        var q = "timed.update_by(ops=[contains_row, before_row, after_row])";
        runner.test("RollingProdTime- 3 Ops No Groups", q, "num1", "timestamp");
    }

    @Test
    void rollingProdTime1Group3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1'])";
        runner.test("RollingProdTime- 3 Ops 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingProdTime2Groups3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2'])";
        runner.test("RollingProdTime- 3 Ops 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingProdTime3Groups3Ops() {
        var q = "timed.update_by(ops=[contains_row, before_row, after_row], by=['key1','key2','key3'])";
        runner.test("RollingProdTime- 3 Ops 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "timestamp");
    }

}
