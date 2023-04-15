/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URL;
import java.nio.file.*;
import org.junit.Test;

public class FilerTest {

    @Test
    public void getFileText() throws Exception {
        Path p = Paths.get(getClass().getResource("filertest.txt").toURI());
        assertEquals("One Two Three\nFour Five Six", Filer.getFileText(p), "Wrong file text");
    }

    @Test
    public void getUrlText() throws Exception {
        URL url = getClass().getResource("filertest.txt");
        assertEquals("One Two Three\nFour Five Six", Filer.getURLText(url), "Wrong file text");
    }

}
