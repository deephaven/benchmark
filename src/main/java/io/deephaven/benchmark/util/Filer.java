/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Utility class for manipulating files and directories.
 */
public class Filer {

    /**
     * Delete the given directory recursively
     * 
     * @param dir the directory to delete
     */
    static public void deleteAll(Path dir) {
        try {
            if (!Files.exists(dir))
                return;
            Files.walk(dir)
                    .sorted(Comparator.reverseOrder())
                    .map(Path::toFile).forEach(File::delete);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete directory: " + dir);
        }
    }

    /**
     * Read the text of a file while preserving newlines and getting rid of carriage returns
     * 
     * @param file the file to read
     * @return the text of the file trimmed and carriage-return-less
     */
    static public String getFileText(Path file) {
        try {
            return new String(Files.readString(file)).replace("\r", "").trim();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get text contents of file: " + file, ex);
        }
    }

    /**
     * Write the given text to a file. Create if the file does not exist. Overwrite if it exists.
     * 
     * @param file the file to contain the text
     * @param text the text to write to the file
     */
    static public void putFileText(Path file, String text) {
        try (BufferedWriter out = Files.newBufferedWriter(file)) {
            out.append(text);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to put text to file: " + file, ex);
        }
    }

    /**
     * Read the text of a URL while preserving newlines and getting rid of carriage returns
     * 
     * @param url the URL to read
     * @return the text of the file trimmed and carriage-return-less
     */
    static public String getURLText(URL url) {
        try (InputStream in = url.openStream()) {
            return new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n")).trim();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get text contents of url: " + url, ex);
        }
    }

}
