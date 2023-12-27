/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.controller;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

public class DeephavenDockerControllerTest {

    @Test
    void parseContainerIds() {
        var dockerPsStr = """
        CONTAINER ID   IMAGE                                             COMMAND                  CREATED       STAT                                                                                                                                                               NAMES
        34e3c5866046   ghcr.io/deephaven/server:edge                     "/opt/deephaven/serv…"   2 hours ago   Up 2                                                                                                                       deephaven-edge-deephaven-1
        e751ca90644e   docker.redpanda.com/vectorized/redpanda:v22.2.5   "/entrypoint.sh redp…"   2 hours ago   Up 2

        """;
        var c = new DeephavenDockerController(null, null);
        var ids = c.parseContainerIds(dockerPsStr);
        assertEquals("[34e3c5866046]", ids.toString(), "Wrong deephaven container ids");
        ids = c.parseContainerIds("Nothing");
        assertTrue(ids.isEmpty(), "No container ids should have been found");
    }

    @Test
    void parseContainerInfo() {
        var dockerInspectStr = """
        {"Image": "sha256:d9187a1d0db6adfcb4e35617e3a3205053cd091e3e2f396ae1a9b08440d65f78",}
        "ResolvConfPath": "/var/lib/docker/containers/04cf278a6a739476cce9e8393f07538c75c8b1ac87c58351d/resolv.conf",
        "HostName": "deephaven",
        
             "Name": "/deephaven-edge-deephaven-1",
        "RestartCount": 0,
        "Driver": "overlay2",
        "com.docker.compose.project.config_files": "/home/stan/Deephaven/deephaven-edge/docker-compose.yml",
        """;
        var c = new DeephavenDockerController(null, null);
        var info = c.parseContainerInfo(dockerInspectStr);
        assertEquals("/deephaven-edge-deephaven-1", info.name(), "Wrong deephaven container name");
        assertEquals("/home/stan/Deephaven/deephaven-edge/docker-compose.yml", info.composePath(),
                "Wrong deephaven container compose uri");
    }

}
