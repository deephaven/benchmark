/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import java.time.Instant;
import org.junit.jupiter.api.*;

public class DatesTest {
    @Test
    public void formatDate() {
        var millisUtc = Long.valueOf(1695838560288L);
        var instantUtc = Instant.ofEpochMilli(millisUtc);
        var isoUtc = instantUtc.toString();

        assertNull(Dates.formatDate(null, null), "Should be null");
        assertEquals("2023-09-27", Dates.formatDate(millisUtc, "yyyy-MM-dd"));
        assertEquals("09/27/2023 18:16:00", Dates.formatDate(instantUtc, "MM/dd/yyyy HH:mm:ss"));
        assertEquals("2023-09-27T06:16:00.288Z", Dates.formatDate(isoUtc, "yyyy-MM-dd'T'hh:mm:ss.SSSX"));
    }

}
