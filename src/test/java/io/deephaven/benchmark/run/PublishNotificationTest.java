/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import io.deephaven.benchmark.connect.CachedResultTable;
import io.deephaven.benchmark.util.Filer;

public class PublishNotificationTest {
    String csv = """
    Static_Benchmark|Start_Time|End_Time|Days|Variability|Rate|Change
    Update- 2 Calcs Using Int|2023-09-08|2023-09-23|15|34.6%|79,548,166|-50.6%
    Vector- 5 Calcs 1M Groups Dense Data|2023-09-08|2023-09-23|15|9.7%|37,505,860|-15.5%
    CumMin- No Groups 1 Cols|2023-09-08|2023-09-23|15|7.5%|175,264,357|-12.5%
    """;

    String plat = """
    run_id|origin|name|value
    1b6be4c0b1|test-runner|available.processors|8
    1b6be4c0b1|deephaven-engine|available.processors|16
    1b6be4c0b1|deephaven-engine|java.max.memory|24g
    1b6be4c0b1|test-runner|deephaven.version|0.28.0
    1b6be4c0b1|deephaven-engine|deephaven.version|0.29.0
    """;

    @Test
    public void generateSvg() throws Exception {
        URL svgTemplate = getClass().getResource("test-publish.template.svg");
        Path svgFile = Paths.get(svgTemplate.toURI()).getParent().resolveSibling("worst_since_last_version.svg");
        String svgTemp = Filer.getURLText(svgTemplate);
        var p = new PublishNotification(null, svgTemplate, svgFile.getParent());
        var t = CachedResultTable.create(plat, "|");
        svgTemp = p.updatePlatformDetails(t, svgTemp);
        t = CachedResultTable.create(csv, "|");
        p.generateSvg(t, svgTemp, svgFile.getParent(), svgFile.getFileName().toString());
        assertEquals("""
        <svg viewBox="0 0 170 95" xmlns="http://www.w3.org/2000/svg">
          <style>
            div {
              color: green;
              font: 3.0px fira sans,sans-serif;
            }
          </style>
          <foreignObject x="0" y="0" width="100%" height="100%">
            <div xmlns="http://www.w3.org/1999/xhtml">
              <table><tr><th>Deephaven</th><th>Summary</th><th>2023-09-24</th></tr></table>
              <table cellspacing="0">
                <thead>
                  <tr><th>Static_Benchmark</th><th>Start_Time</th><th>End_Time</th><th>Days</th><th>Variability</th><th>Rate</th><th>Change</th></tr>
                </thead>
                <tbody>
                  <tr><td>Update- 2 Calcs Using Int</td><td>2023-09-08</td><td>2023-09-23</td><td>15</td><td>34.6%</td><td>79,548,166</td><td>-50.6%</td></tr>
                  <tr><td>Vector- 5 Calcs 1M Groups Dense Data</td><td>2023-09-08</td><td>2023-09-23</td><td>15</td><td>9.7%</td><td>37,505,860</td><td>-15.5%</td></tr>
                  <tr><td>CumMin- No Groups 1 Cols</td><td>2023-09-08</td><td>2023-09-23</td><td>15</td><td>7.5%</td><td>175,264,357</td><td>-12.5%</td></tr>
                </tbody>
                <tfoot>
                  <tr><td colspan="3">* cpu-threads=16 heap=24g os=ubuntu-22.04.1-lts full-benchmark-set=${benchmark_count}</td></tr>
                </tfoot>
              </table>
            </div>
          </foreignObject>
        </svg>
        """.replace("\r", "").trim(), Filer.getFileText(svgFile).replaceAll(
                "<th>[0-9]{4}[-][0-9]{2}[-][0-9]{2}</th>", "<th>2023-09-24</th>"));
    }

    @Test
    public void generateCsv() throws Exception {
        URL svgTemplate = getClass().getResource("test-publish.template.svg");
        Path csvFile = Paths.get(svgTemplate.toURI()).getParent().resolveSibling("worst_since_last_version.csv");
        var p = new PublishNotification(null, svgTemplate, csvFile.getParent());
        var t = CachedResultTable.create(plat, "|");
        t = CachedResultTable.create(csv, "|");
        p.generateCsv(t, csvFile.getParent(), csvFile.getFileName().toString());
        assertEquals("""
        Static_Benchmark                      Start_Time    End_Time  Days  Variability         Rate  Change
        Update- 2 Calcs Using Int             2023-09-08  2023-09-23    15        34.6%   79,548,166  -50.6%
        Vector- 5 Calcs 1M Groups Dense Data  2023-09-08  2023-09-23    15         9.7%   37,505,860  -15.5%
        CumMin- No Groups 1 Cols              2023-09-08  2023-09-23    15         7.5%  175,264,357  -12.5%
        """.replace("\r", "").trim(), Filer.getFileText(csvFile));
    }

}
