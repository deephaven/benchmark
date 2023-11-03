/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class ExecTest {
    @Test
    public void exec() {
        var os = System.getProperty("os.name");
        var cmd = os.contains("Windows") ? "cmd /c echo Ack" : "echo Ack";
        assertEquals("Ack", Exec.exec(Paths.get("."), cmd), "Wrong response");
    }

}
