/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.net.*;
import java.util.concurrent.TimeUnit;

/**
 * Utils for executing processes from the command line.
 * </p>
 * Note: No effort has been made to make this secure
 */
public class Exec {
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
        exec("sudo docker compose -f " + dockerComposeFile + " down --timeout 0");
        exec("sudo docker compose -f " + dockerComposeFile + " up -d");
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

}
