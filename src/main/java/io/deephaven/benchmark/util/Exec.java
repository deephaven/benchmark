/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.io.*;
import java.util.concurrent.TimeUnit;

/**
 * Utils for executing processes from the command line.
 * </p>
 * Note: No effort has been made to make this secure or prevent any horror.ss
 */
public class Exec {
    /**
     * Restart a docker container using <code>docker compose</code>. If the given compose file is blank skip.
     * 
     * @param dockerComposeFile the path to the relevant docker-compose.yml
     */
    static public void restartDocker(String dockerComposeFile) {
        if (dockerComposeFile.isBlank())
            return;
        exec("sudo docker compose -f " + dockerComposeFile + " down");
        Threads.sleep(1000);
        exec("sudo docker compose -f " + dockerComposeFile + " up -d");
        Threads.sleep(3000);
    }

    /**
     * Blindly execute a command in whatever shell Java decides is relevant. Throw exceptions on timeout, non-zero exit
     * code, or other general failures.
     * 
     * @param command the shell command to run
     * @return stdout and stderr separated by newlines
     */
    static public int exec(String command) {
        try {
            Process process = Runtime.getRuntime().exec(command);
            if (!process.waitFor(20, TimeUnit.SECONDS))
                throw new RuntimeException("Timeout while running command: " + command);
            if (process.exitValue() != 0)
                throw new RuntimeException("Bad exit code " + process.exitValue() + " for command: " + command);
            return process.exitValue();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to execute command: " + command, ex);
        }
    }

    static String copyToString(InputStream input) throws Exception {
        var out = new StringWriter();
        try (InputStream in = input) {
            for (int i = 0; i < in.available(); i++)
                out.write(in.read());
            return out.toString();
        }
    }

}
