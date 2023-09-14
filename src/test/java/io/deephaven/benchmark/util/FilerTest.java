/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import java.net.URL;
import java.nio.file.*;
import org.junit.Test;

public class FilerTest {
    @Test
    public void delete() throws Exception {
        Path p = Paths.get(getClass().getResource("filertest.txt").toURI()).resolveSibling("deleteme.txt");
        Filer.putFileText(p, "This should be deleted soon");
        assertTrue(Files.exists(p), "Must exist in order to be deleted");

        Filer.delete(p);
        assertFalse(Files.exists(p), "Failed to delete file");

        Path dir = p.resolveSibling("test-dir");
        Path dir2 = dir.resolve("test-sub-dir");
        Path file1 = dir2.resolve("deleteme.txt");
        Files.createDirectories(dir2);
        Filer.putFileText(file1, "This should be deleted soon");
        assertTrue(Files.exists(file1), "Must exist in order to be deleted");

        Filer.delete(dir);
        assertFalse(Files.exists(dir), "Failed to delete dir");
        assertFalse(Files.exists(file1), "Failed to delete file");
    }

    @Test
    public void getFileText() throws Exception {
        Path p = Paths.get(getClass().getResource("filertest.txt").toURI());
        assertEquals("One Two Three\nFour Five Six", Filer.getFileText(p), "Wrong file text");
    }

    @Test
    public void putFileText() throws Exception {
        Path p = Paths.get(getClass().getResource("filertest.txt").toURI());
        p = p.resolveSibling("filtertest2.txt");
        Filer.putFileText(p, "One Two\nThree Four");
        assertEquals("One Two\nThree Four", Files.readString(p), "Wrong file text");
    }

    @Test
    public void getUrlText() throws Exception {
        URL url = getClass().getResource("filertest.txt");
        assertEquals("One Two Three\nFour Five Six", Filer.getURLText(url), "Wrong file text");
    }

}
