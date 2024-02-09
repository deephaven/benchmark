/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.io.*;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A utility for executing a process on the command line. It will run any command, regardless of how dangerous.
 */
public class Exec {
    /**
     * Blindly execute a command in whatever shell Java decides is relevant. Throw exceptions on timeout, non-zero exit
     * code, or other general failures.
     * 
     * @param command the shell command to run
     * @return the standard output of the process
     */
    static public String exec(Path workingDir, String... command) {
        try {
            Process process = Runtime.getRuntime().exec(command, null, workingDir.toFile());
            var out = getStdout(process);
            if (!process.waitFor(20, TimeUnit.SECONDS))
                throw new RuntimeException("Timeout while running command: " + command);
            if (process.exitValue() != 0)
                throw new RuntimeException("Bad exit code " + process.exitValue() + " for command: " + command);
            return out;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute command: " + command, ex);
        }
    }

    /**
     * Get the text value of the standard out of a running Process
     * 
     * @param process a running Process
     * @return the standard output of a running process
     */
    static String getStdout(Process process) {
        try (BufferedReader in = process.inputReader()) {
            return in.lines().collect(Collectors.joining("\n"));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get stdout from pid: " + process.info(), ex);
        }
    }

}
