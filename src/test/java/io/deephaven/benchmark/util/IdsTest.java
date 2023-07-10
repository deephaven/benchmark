/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.LinkedHashSet;
import org.junit.jupiter.api.*;

public class IdsTest {
    @Test
    public void uniqueName() {
        var ids = new LinkedHashSet<String>();
        int count = 100000;
        for (int i = 0; i < count; i++) {
            ids.add(Ids.uniqueName());
        }
        assertEquals(count, ids.size(), "Wrong unique count");

        for (String id : ids) {
            assertTrue(id.matches("[A-Za-z0-9_-]+[.][a-f0-9]+[.][A-Za-z0-9_-]+"), "Wrong id format");
        }
    }

    @Test
    public void getFileSafeName() {
        assertEquals("This_is_a_test", Ids.getFileSafeName("This is a test"), "Wrong safe name");
    }

    @Test
    public void runIds() {
        String id = Ids.runId();
        assertTrue(id.matches("run-[a-z0-9]{10,11}"), "Bad run id: " + id);
    }

    @Test
    public void isRunId() {
        String recent = "run-1619d760fa";
        String max = "run-7ffffe90a0f44c7f";

        assertFalse(Ids.isRunId("run-abcdefghi"), "Should not be valid id");
        assertFalse(Ids.isRunId("1619d1a435"), "Should not be valid id");
        assertTrue(Ids.isRunId(recent), "Should be valid id");
        assertTrue(Ids.isRunId(max), "Should be valid id");
    }

    @Test
    public void hash64() {
        assertEquals(65, Ids.hash64("A"));
        assertEquals(59610663905L, Ids.hash64("AAAAAAA"));
        assertEquals(5448659364008734959L, Ids.hash64("col1:int:[1-100]"));
    }

}
