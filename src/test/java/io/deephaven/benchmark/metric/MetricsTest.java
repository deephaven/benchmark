/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.metric;

import static org.junit.jupiter.api.Assertions.*;
import java.util.TreeMap;
import org.junit.jupiter.api.*;

public class MetricsTest {
    @Test
    public void set() {
        Metrics m = new Metrics("origin1", "cat1");
        m.set("mname1", 1);
        assertEquals(1, m.getValue("mname1"), "Wrong metric value");
        assertNull(m.getValue("missing"), "Metric should be missing");
        m.set("mname1", 2, "note1", "note2");
        assertEquals(2, m.getValue("mname1"), "Wrong metric value");
        assertEquals("note1;note2", m.getNote("mname1"), "Wrong notes");
    }

    @Test
    public void tostring() {
        Metrics m = new Metrics(123, "o1", "c1");
        assertEquals("{timestamp=123, origin=o1, category=c1}", m.toString(), "Wrong toString");
        m.set("n1", 10);
        assertEquals("{timestamp=123, origin=o1, category=c1, n1=10}", m.toString(), "Wrong toString");
        m.set("n1", 10, "n1", "n2");
        assertEquals("{timestamp=123, origin=o1, category=c1, n1=10;n1;n2}", m.toString(), "Wrong toString");
    }

    @Test
    public void getMetric() {
        Metrics m = new Metrics(123, "o1", "c1");
        m.set("n1", 10);
        var r = new TreeMap<>(m.getMetric("n1")).toString();
        assertEquals("{name=c1.n1, note=, origin=o1, timestamp=123, value=10}", r,
                "Wrong map result");

        m.set("n1", 10, "n1", "n2");
        r = new TreeMap<>(m.getMetric("n1")).toString();
        assertEquals("{name=c1.n1, note=n1;n2, origin=o1, timestamp=123, value=10}", r,
                "Wrong map result");
    }

}
