/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.controller;

import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import io.deephaven.benchmark.util.Exec;
import io.deephaven.benchmark.util.Threads;

/**
 * A <code>Controller</code> implementation that handes docker start, stop and logging through
 * <code>docker compose</code> calls. This implementation does not handle remote docker calls and requires that the
 * service be on the same system as the controller.
 */
public class DeephavenDockerController implements Controller {
    final String composePropPath;
    final String httpHostPort;
    final Path workDir;

    /**
     * Make a Deephaven <code>Controller</code> instance for starting/stopping a local instance of Deephaven.
     * 
     * @param composePath the path to the <code>docker-compose.yml</code> file or null
     * @param httpHostPort HTTP host and port for checking availability or null (ex deephaven.addr=localhost:10000)
     */
    public DeephavenDockerController(String composePath, String httpHostPort) {
        this.composePropPath = (composePath == null) ? "" : composePath.trim();
        this.httpHostPort = (httpHostPort == null) ? "" : httpHostPort.trim();
        this.workDir = composePropPath.isBlank() ? Paths.get(".") : Paths.get(composePropPath).getParent();
    }

    /**
     * Start the Deephaven service. If an existing Deephaven service is running, stop it first. If a docker compose file
     * is not specified, do nothing.
     * 
     * @return true if the service was started, otherwise false
     */
    @Override
    public boolean startService() {
        if (composePropPath.isBlank() || httpHostPort.isBlank())
            return false;
        var composeRunPath = getRunningComposePath();
        if (composeRunPath != null)
            exec("sudo docker compose -f " + composeRunPath + " down");
        exec("sudo docker compose -f " + composePropPath + " up -d");
        waitForEngineReady();
        return true;
    }

    /**
     * Stop the Deephaven service and remove the docker container. If no docker compose is specified, do nothing.
     * 
     * @return true if the service was stopped, otherwise false
     */
    @Override
    public boolean stopService() {
        if (composePropPath.isBlank())
            return false;
        exec("sudo docker compose -f " + composePropPath + " down --timeout 0");
        return true;
    }

    /**
     * Stop the Deephaven service and start it.
     * 
     * @return true if the service was started, otherwise false
     */
    @Override
    public boolean restartService() {
        stopService();
        return startService();
    }

    /**
     * Get the docker compose log since starting the Deephaven service. If no docker compose is specified, do nothing,
     * since the logs will get progressively bigger without a restart.
     * 
     * @return the text collected from docker compose log
     */
    @Override
    public String getLog() {
        if (composePropPath.isBlank() || httpHostPort.isBlank())
            return "";
        var composePath = getRunningComposePath();
        if (composePath != null)
            return exec("sudo docker compose -f " + composePath + " logs");
        return "";
    }

    void waitForEngineReady() {
        long beginTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - beginTime < 10000) {
            if (getUrlStatus("http://" + httpHostPort + "/ide/"))
                return;
            Threads.sleep(100);
        }
        throw new RuntimeException("Timed out waiting for Deephaven Engine to start");
    }

    boolean getUrlStatus(String uri) {
        try {
            var url = createUrl(uri);
            var connect = url.openConnection();
            if (!(connect instanceof HttpURLConnection))
                return false;
            var httpConn = (HttpURLConnection) connect;
            var code = httpConn.getResponseCode();
            httpConn.disconnect();
            return (code == 200);
        } catch (Exception ex) {
            return false;
        }
    }

    URL createUrl(String uri) {
        try {
            return new URI(uri).toURL();
        } catch (Exception e) {
            throw new RuntimeException("Bad URL: " + uri);
        }
    }

    String getRunningComposePath() {
        var dhContainerIds = getRunningContainerIds();
        if (dhContainerIds.isEmpty())
            return null;
        var containerInfo = getContainerInfo(dhContainerIds.get(0));
        return containerInfo.composePath;
    }

    List<String> getRunningContainerIds() {
        var out = exec("sudo docker ps");
        return parseContainerIds(out);
    }

    ContainerInfo getContainerInfo(String containerId) {
        var out = exec("sudo docker container inspect " + containerId);
        return parseContainerInfo(out);
    }

    List<String> parseContainerIds(String dockerPsStr) {
        return dockerPsStr.lines().filter(s -> s.contains("/deephaven/server"))
                .map(s -> s.replaceAll("^([^ \t]+)[ \t].*$", "$1")).toList();
    }

    ContainerInfo parseContainerInfo(String dockerInspectStr) {
        var name = getPropValue(dockerInspectStr, "Name", "deephaven");
        var composeUri = getPropValue(dockerInspectStr, "com.docker.compose.project.config_files", "compose");
        return new ContainerInfo(name, composeUri);
    }

    String getPropValue(String props, String name, String containsVal) {
        var matchName = "\"" + name + "\":";
        var lines = props.lines().map(s -> s.trim()).filter(s -> s.startsWith(matchName) && s.contains(containsVal));
        var matches = lines.toList();
        if (matches.size() < 1)
            throw new RuntimeException("Failed to find docker property: " + name + " containing: " + containsVal);
        return matches.get(0).replaceAll("\"[,]?", "").split("[:]\s*")[1].trim();
    }

    String exec(String command) {
        return Exec.exec(workDir, command);
    }

    record ContainerInfo(String name, String composePath) {
    };

}
