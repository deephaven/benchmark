/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.*;
import io.deephaven.benchmark.util.Timer;

public class BenchResultTest {
    final private Path parent = getResourceParent();

    @Test
    public void single() throws Exception {
        BenchResult result = new BenchResult(parent, "test-result.csv");
        result.setName("mytest");

        Files.deleteIfExists(result.file);
        assertFalse(Files.exists(result.file), "Result file exists: " + result.file);

        result.test("deephaven-engine", timer(123), 1234);
        Thread.sleep(200);
        result.commit();

        assertTrue(Files.exists(result.file), "Missing result file: " + result.file);

        List<String[]> csv = getResult(result);
        assertEquals(2, csv.size(), "Wrong line count");
        assertEquals("[benchmark_name, origin, timestamp, test_duration, op_duration, op_rate, row_count]",
                Arrays.toString(csv.get(0)), "Wrong header");
        assertEquals("mytest", csv.get(1)[0], "Wrong name");
        assertEquals("deephaven-engine", csv.get(1)[1], "Wrong origin");
        assertEquals(result.timer.beginTime, Long.parseLong(csv.get(1)[2]), "Wrong timestamp");
        assertTrue(Double.parseDouble(csv.get(1)[3]) >= 0.200f, "Wrong test duration" + csv.get(1)[3]);
        assertTrue(Double.parseDouble(csv.get(1)[4]) >= 0.12f, "Wrong op duration: " + csv.get(1)[4]);
        assertEquals(10032, Long.parseLong(csv.get(1)[5]), 0.01, "Wrong test rate");
    }

    @Test
    public void multi() throws Exception {
        BenchResult result = new BenchResult(parent, "test-result.csv");
        result.setName("mytest");

        Files.deleteIfExists(result.file);
        assertFalse(Files.exists(result.file), "Result file exists: " + result.file);

        result.test("deephaven-engine", timer(123), 1234);
        result.commit();

        result = new BenchResult(parent, "test-result.csv");
        result.setName("mytest2");

        result.test("deephaven-engine", timer(321), 2345);
        result.commit();

        assertTrue(Files.exists(result.file), "Missing result file: " + result.file);

        List<String[]> csv = getResult(result);
        assertEquals(3, csv.size(), "Wrong line count");
        assertEquals("[benchmark_name, origin, timestamp, test_duration, op_duration, op_rate, row_count]",
                Arrays.toString(csv.get(0)), "Wrong header");
        assertEquals("mytest", csv.get(1)[0], "Wrong name");
        assertEquals("mytest2", csv.get(2)[0], "Wrong name");
    }

    private Path getResourceParent() {
        try {
            return Paths.get(getClass().getResource("test-profile.properties").toURI()).getParent();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get resource dir", ex);
        }
    }

    private List<String[]> getResult(BenchResult result) throws Exception {
        return Files.lines(result.file).map(v -> v.split(",")).toList();
    }

    private Timer timer(int millis) {
        return new DTimer(millis);
    }

    static class DTimer extends Timer {
        Duration duration;

        DTimer(int durationMillis) {
            this.duration = Duration.ofMillis(durationMillis);
        }

        @Override
        public Duration duration() {
            return duration;
        }
    }

}
