/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.io.*;
import java.net.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * Utils for executing processes from the command line.
 * </p>
 * Note: No effort has been made to make this secure
 */
public class Exec {

    /**
     * Get the docker logs currently available from the deephaven container
     * 
     * @param dockerComposeFile the path to the relevant docker-compose.yml
     * @return the log output with timestamps or null if no compose file is provided
     */
    static public String getDockerLog(String dockerComposeFile) {
        if (dockerComposeFile.isBlank())
            return null;
        var composeDir = Paths.get(dockerComposeFile).getParent();
        var logOpts = "--no-log-prefix --no-color deephaven";
        return exec(composeDir, "sudo docker compose logs " + logOpts);
    }

    /**
     * Stop the docker services defined in the given docker compose file
     * 
     * @param dockerComposeFile the path to the relevant docker-compose.yml
     * @return true if docker was successfully stopped, otherwise false
     */
    // -XX:+UnlockDiagnosticVMOptions -XX:+PrintInlining"
    static public boolean stopDocker(String dockerComposeFile) {
        if (dockerComposeFile.isBlank())
            return false;
        var composeDir = Paths.get(dockerComposeFile).getParent();
        var out = exec(composeDir, "sudo docker compose -f " + dockerComposeFile + " down --timeout 0");
        return out != null;
    }

    static public boolean startDocker(String dockerComposeFile, String deephavenHostPort) {
        if (dockerComposeFile.isBlank() || deephavenHostPort.isBlank())
            return false;
        var composeDir = Paths.get(dockerComposeFile).getParent();
        exec(composeDir, "sudo docker compose -f " + dockerComposeFile + " up -d");
        long beginTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - beginTime < 10000) {
            var status = getUrlStatus("http://" + deephavenHostPort + "/ide/");
            if (status)
                return true;
            Threads.sleep(100);
        }
        return false;
    }


    /**
     * Restart a docker container using <code>docker compose</code>. If the given compose file property is blank skip.
     * 
     * @param dockerComposeFile the path to the relevant docker-compose.yml
     * @param deephavenHostPort the host:port of the Deephaven service
     * @return true if attempted docker restart, otherwise false
     */
    static public boolean restartDocker(String dockerComposeFile, String deephavenHostPort) {
        if (dockerComposeFile.isBlank() || deephavenHostPort.isBlank())
            return false;
        stopDocker(dockerComposeFile);
        return startDocker(dockerComposeFile, deephavenHostPort);
    }

    /**
     * Blindly execute a command in whatever shell Java decides is relevant. Throw exceptions on timeout, non-zero exit
     * code, or other general failures.
     * 
     * @param command the shell command to run
     * @return stdout and stderr separated by newlines
     */
    static public String exec(Path workingDir, String command) {
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

    static boolean getUrlStatus(String uri) {
        var url = createUrl(uri);
        try {
            var connect = url.openConnection();
            if (!(connect instanceof HttpURLConnection))
                return false;
            var code = ((HttpURLConnection) connect).getResponseCode();
            return (code == 200);
        } catch (Exception ex) {
            return false;
        }
    }

    static URL createUrl(String uri) {
        try {
            return new URL(uri);
        } catch (MalformedURLException e) {
            throw new RuntimeException("Bad URL: " + uri);
        }
    }

    static String getStdout(Process process) {
        try (BufferedReader in = process.inputReader()) {
            return in.lines().collect(Collectors.joining("\n"));
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get stdout from pid: " + process.info(), ex);
        }
    }

}
