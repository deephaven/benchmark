/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.updateby;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the updateBy table operation. Defines a time-based rolling product. The result table contains
 * additional columns with windowed rolling products for each specified column in the source table. *
 * <p>
 * Note: This test must contain benchmarks and <code>rev_time/fwd_time</code> that are comparable to
 * <code>RollingProdTickTest</code>
 */
public class RollingProdTimeTest {
    final StandardTestRunner runner = new StandardTestRunner(this);
    final Setup setup = new Setup(runner);

    @BeforeEach
    void setup() {
        runner.tables("timed");
    }

    @Test
    void rollingProdTime0Group3Ops() {
        runner.setScaleFactors(20, 12);
        setup.rollTime0Groups("rolling_prod_time");
        var q = "timed.update_by(ops=[contains_row])";
        runner.test("RollingProdTime- No Groups 1 Col", q, "num1", "timestamp");
    }

    @Test
    void rollingProdTime1Group3Ops() {
        runner.setScaleFactors(8.8, 3.5);
        setup.rollTime1Group("rolling_prod_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1'])";
        runner.test("RollingProdTime- 1 Group 100 Unique Vals", q, "key1", "num1", "timestamp");
    }

    @Test
    void rollingProdTime2Groups3Ops() {
        runner.setScaleFactors(4.1, 1.8);
        setup.rollTime2Groups("rolling_prod_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2'])";
        runner.test("RollingProdTime- 2 Groups 10K Unique Combos", q, "key1", "key2", "num1", "timestamp");
    }

    @Test
    void rollingProdTime3Groups3Ops() {
        runner.setScaleFactors(2.5, 0.35);
        setup.rollTime3Groups("rolling_prod_time");
        var q = "timed.update_by(ops=[contains_row], by=['key1','key2','key3'])";
        runner.test("RollingProdTime- 3 Groups 100K Unique Combos", q, "key1", "key2", "key3", "num1",
                "timestamp");
    }

}
