/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.jfr;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

@Disabled
public class TestJfrReader {

    @Test
    public void getEventNames() {
        JfrReader jfr = new JfrReader(getClass().getResource("server.jfr"));
        System.out.println("" + jfr.getEventNames().toString());
    }

}
