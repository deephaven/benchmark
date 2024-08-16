/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import org.junit.jupiter.api.*;

public class StringsTest {
    @Test
    void toArray() {
        assertEquals("String[]", Strings.toArray("one", 2).getClass().getSimpleName());
        var arr = Arrays.asList(Strings.toArray("one", 2));
        assertEquals("[one, 2]", "" + arr);
        assertEquals("[one, 2, one, 2]", "" + Arrays.asList(Strings.toArray("one", 2, arr)));
    }

    @Test
    void startsWith() {
        var available = List.of("four", "three", "two", "one");
        var prefixes = List.of("tw", "th");
        assertEquals("[two, three]", Strings.startsWith(available, prefixes).toString());
    }

}
