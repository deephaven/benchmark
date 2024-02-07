/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

public class ExecTest {
    String[] windowsEcho = {"cmd", "/c", "echo", "Ack"};
    String[] bashEcho = {"echo", "Ack"};

    @Test
    public void exec() {
        var os = System.getProperty("os.name");
        var cmd = os.contains("Windows") ? windowsEcho : bashEcho;
        assertEquals("Ack", Exec.exec(Paths.get("."), cmd), "Wrong response");
    }

}
