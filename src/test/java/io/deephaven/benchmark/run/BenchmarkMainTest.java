/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

public class BenchmarkMainTest {

    @Test
    public void main1() {
        int exitCode = BenchmarkMain.main1(new String[] {"--list-engines"});
        assertEquals(0, exitCode, "Wrong exit code");
    }

}
