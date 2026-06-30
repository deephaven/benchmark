/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static java.nio.file.StandardOpenOption.*;
import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Utility class for manipulating files and directories.
 */
public class Filer {
    /**
     * Delete the given file or directory. Directories are deleted recursively
     * 
     * @param path the directory to delete
     */
    static public void delete(Path path) {
        try {
            if (!Files.exists(path))
                return;
            if (!Files.isDirectory(path)) {
                Files.deleteIfExists(path);
            } else {
                Files.walk(path)
                        .sorted(Comparator.reverseOrder())
                        .map(Path::toFile).forEach(File::delete);
            }
        } catch (Exception ex) {
            throw new RuntimeException("Failed to delete path: " + path);
        }
    }

    /**
     * Create a file with the given name in the given parent directory. Create the parent directory if it does not
     * exist. Permissions are 755 for directories and 644 for files.
     * 
     * @param parentDir the parent directory to contain the file
     * @param fileName the name of the file to create
     * @return the path of the created file
     */
    static public Path createFile(String parentDir, String fileName) {
        try {
            return Files.createFile(createDirectory(parentDir).resolve(fileName),
                    PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-r--r--")));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create file: " + fileName, ex);
        }
    }

    /**
     * Create a directory with the given name. Create parent directories if they do not exist. Permissions are 755.
     * 
     * @param dir the directory to create
     * @return the path of the created directories
     */
    static public Path createDirectory(String dir) {
        try {
            return Files.createDirectories(Paths.get(dir), PosixFilePermissions.asFileAttribute(
                    PosixFilePermissions.fromString("rwxr-xr-x")));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create temp directory: " + dir, ex);
        }
    }

    /**
     * Get the size of a file or directory in bytes. Directory sizes are calculated recursively by summing the sizes of
     * all regular files contained within.
     * 
     * @param file the file or directory to get the size of
     * @return the size of the file or directory in bytes
     */
    static public long getByteSize(String path) {
        try {
            return Files.walk(Paths.get(path)).filter(Files::isRegularFile).mapToLong(f -> f.toFile().length()).sum();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get size of file: " + path, ex);
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
    static public void putFileText(Path file, CharSequence text) {
        try (BufferedWriter out = Files.newBufferedWriter(file, CREATE, WRITE, TRUNCATE_EXISTING)) {
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
