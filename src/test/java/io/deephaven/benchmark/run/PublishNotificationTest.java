/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        Path svgFile = Paths.get(svgTemplate.toURI()).getParent().resolveSibling("nightly_worst_rate_change.svg");
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
    public void generateTableBlock() throws Exception {
        URL svgTemplate = getClass().getResource("test-publish.template.svg");
        Path jsonFile = Paths.get(svgTemplate.toURI()).getParent().resolveSibling("nightly_worst_rate_change.json");
        var p = new PublishNotification(null, svgTemplate, jsonFile.getParent());
        var t = CachedResultTable.create(csv, "|");
        p.generateTableBlock(t, "Worst Scores by Scr", jsonFile.getParent(), jsonFile.getFileName().toString());
        assertEquals("{\"type\": \"table\", \"column_settings\": [{\"align\": \"left\", \"is_wrapped\": false}"
                + ", {\"align\": \"right\", \"is_wrapped\": false}".repeat(6) + "], \"rows\": [\n"
                + cells(false, "Static_Benchmark (Worst Scores by Scr)", "Start_Time", "End_Time", "Days", "Variability", "Rate", "Change") + ",\n"
                + cells(true, "Update- 2 Calcs Using Int", "2023-09-08", "2023-09-23", "15", "34.6%", "79,548,166", "-50.6%") + ",\n"
                + cells(true, "Vector- 5 Calcs 1M Groups Dense Data", "2023-09-08", "2023-09-23", "15", "9.7%", "37,505,860", "-15.5%") + ",\n"
                + cells(true, "CumMin- No Groups 1 Cols", "2023-09-08", "2023-09-23", "15", "7.5%", "175,264,357", "-12.5%")
                + "]}", Filer.getFileText(jsonFile));
    }

    @Test
    public void getSlackPayload() throws Exception {
        URL svgTemplate = getClass().getResource("test-publish.template.svg");
        Path outDir = Paths.get(svgTemplate.toURI()).getParent().resolveSibling("slack-payload");
        Files.createDirectories(outDir);
        var p = new PublishNotification(null, svgTemplate, outDir);
        p.releaseVersion = "42";
        var t = CachedResultTable.create("""
        Benchmark,Dif5d,DifRls,Var5d
        S Sort- "Quoted" a|b *c* d_e,-1.0%,-2.0%,0.5%
        """, ",");
        p.generateTableBlock(t, "Worst Scores by Scr", outDir, "nightly_worst_score.json");
        p.generateTableBlock(t, "Best Scores by Scr", outDir, "nightly_best_score.json");
        var table = """
        {"type": "table", "column_settings": [{"align": "left", "is_wrapped": false}, {"align": "right", "is_wrapped": false}, \
        {"align": "right", "is_wrapped": false}, {"align": "right", "is_wrapped": false}], "rows": [
        [{"type": "raw_text", "text": "Benchmark (%s)"}, {"type": "raw_text", "text": "\u03945d"}, \
        {"type": "raw_text", "text": "\u039442"}, {"type": "raw_text", "text": "\u03c35d"}],
        [{"type": "rich_text", "elements": [{"type": "rich_text_section", "elements": \
        [{"type": "text", "text": "S Sort- \\"Quoted\\" a|b *c* d_e", "style": {"code": true}}]}]}, \
        {"type": "raw_text", "text": "-1.0%%"}, {"type": "raw_text", "text": "-2.0%%"}, {"type": "raw_text", "text": "0.5%%"}]]}""";
        assertEquals("{\"channel\": \"\", \"unfurl_links\": false, \"unfurl_media\": false, "
                + "\"text\": \"Nightly Benchmark Changes\", \"blocks\": [{\"type\": \"section\", \"text\": {\"type\": \"mrkdwn\", "
                + "\"text\": \"Nightly Benchmark Changes <https://controller.try-dh.demo.community.deephaven.io/get_ide|(Benchmark Tables)>\"}},\n"
                + table.formatted("Worst Scores by Scr") + ",\n" + table.formatted("Best Scores by Scr") + "]}",
                p.getSlackPayload(outDir));
    }

    private String cells(boolean codeFirst, String... values) {
        var cells = new ArrayList<String>();
        for (int i = 0; i < values.length; i++) {
            cells.add((codeFirst && i == 0)
                    ? "{\"type\": \"rich_text\", \"elements\": [{\"type\": \"rich_text_section\", \"elements\": "
                            + "[{\"type\": \"text\", \"text\": \"" + values[i] + "\", \"style\": {\"code\": true}}]}]}"
                    : "{\"type\": \"raw_text\", \"text\": \"" + values[i] + "\"}");
        }
        return "[" + String.join(", ", cells) + "]";
    }

}
