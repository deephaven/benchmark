/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class ExecTest {
    @Test
    public void exec() {
        var os = System.getProperty("os.name");
        var cmd = os.contains("Windows") ? "cmd /c echo Ack" : "echo Ack";
        assertEquals(0, Exec.exec(cmd), "Wrong response");
    }

}