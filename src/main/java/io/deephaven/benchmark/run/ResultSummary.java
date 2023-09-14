/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static java.nio.file.StandardOpenOption.*;
import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.util.Ids;

class ResultSummary {
    static final String headerPrefix = "run-id";
    final Path rootDir;
    final Path summaryFile;

    ResultSummary(Path rootDir, String resultFileName) {
        this.rootDir = rootDir;
        this.summaryFile = getSummaryFile(rootDir, resultFileName);
    }

    URL summarize() {
        if (!Files.exists(rootDir)) {
            System.out.println("Skipping summary because of missing output directory: " + rootDir);
            return null;
        }
        try (BufferedWriter out = Files.newBufferedWriter(summaryFile, CREATE, WRITE, TRUNCATE_EXISTING)) {
            boolean isHeaderWritten = false;
            for (Path resultFile : getResultFiles(rootDir)) {
                String runId = resultFile.getParent().getFileName().toString();
                List<String> lines = Files.lines(resultFile).toList();
                for (int i = 0, n = lines.size(); i < n; i++) {
                    if (!isHeaderWritten)
                        isHeaderWritten = writeSummaryHeader(lines.get(i), out);
                    else if (i == 0)
                        continue;
                    else
                        writeSummaryLine(runId, lines.get(i), out);
                } ;
            }
            return summaryFile.toUri().toURL();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write summary results: " + summaryFile, ex);
        }
    }

    boolean writeSummaryHeader(String header, BufferedWriter out) throws Exception {
        out.write(headerPrefix + ',' + header);
        out.newLine();
        return true;
    }

    void writeSummaryLine(String runTimestamp, String resultLine, BufferedWriter out) throws Exception {
        out.write(runTimestamp.replace("run-", "") + ',');
        out.write(resultLine);
        out.newLine();
    }

    List<Path> getResultFiles(Path rootDir) {
        try {
            List<Path> resultFiles = new ArrayList<>();
            Files.newDirectoryStream(rootDir).forEach(d -> {
                if (!Ids.isRunId(d.getFileName()))
                    return;
                if (!Files.isDirectory(d))
                    return;
                Path resultFile = d.resolve(Bench.resultFileName);
                if (Files.exists(resultFile))
                    resultFiles.add(resultFile);
            });
            Collections.sort(resultFiles, new FileNameComparator());
            return resultFiles;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to get result files from root directory: " + rootDir);
        }
    }

    static Path getSummaryFile(Path parent, String name) {
        Path summaryFile = parent.resolve(name);
        try {
            Files.createDirectories(summaryFile.getParent());
            return summaryFile;
        } catch (Exception ex) {
            throw new RuntimeException("Failed to create results directory" + summaryFile.getParent(), ex);
        }
    }

    static class FileNameComparator implements Comparator<Path> {
        @Override
        public int compare(Path o1, Path o2) {
            return getRunId(o1).compareTo(getRunId(o2));
        }

        String getRunId(Path p) {
            return p.getParent().getFileName().toString();
        }
    }

}
