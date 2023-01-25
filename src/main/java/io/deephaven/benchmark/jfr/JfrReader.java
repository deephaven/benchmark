/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.jfr;

import java.net.URL;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

public class JfrReader {
    final private URL url;

    JfrReader(URL url) {
        this.url = url;
    }

    Set<String> getEventNames() {
        Set<String> names = new HashSet<>();
        try (var jfr = new RecordingFile(Path.of(url.toURI()))) {
            while (jfr.hasMoreEvents()) {
                RecordedEvent event = jfr.readEvent();
                names.add(event.getEventType().getName());
            }
            return names;
        } catch (Exception ex) {
            throw new RuntimeException("Error reading Recording File: " + url, ex);
        }
    }
}

