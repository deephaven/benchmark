/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.standard.client;

import org.junit.jupiter.api.*;
import io.deephaven.benchmark.tests.standard.StandardTestRunner;

/**
 * Standard tests for the ascending sort table operation. Sorts rows of data from the source table according to the
 * defined columns
 */
public class BarrageClientTest {
    final StandardTestRunner runner = new StandardTestRunner(this);

    @BeforeEach
    void setup() {
        runner.tables("source");
        var q = """	
        from deephaven.barrage import barrage_session
        session = barrage_session("localhost", 10000, auth_type="Anonymous")
        """;
        runner.addSetupQuery(q);
    }

    @Test
    void snapshot() {
        runner.setScaleFactors(6, 0);
        var q = "result = session.snapshot(b's/source')";
        runner.test("Client- Barrage Snapshot", q, "key1", "num1");
    }

}
