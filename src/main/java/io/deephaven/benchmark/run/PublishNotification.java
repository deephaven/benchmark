/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Filer;

/**
 * Generate benchmark tables for posting to Slack. Produces tables in Markdown and SVG form. These tables are different from
 * other auto-generated tables in that they pull data exclusively from GCloud.
 */
public class PublishNotification {
    static final String rowIndent = " ".repeat(10);
    final String[] tables = {"nightly_worst_score", "nightly_best_score"};
    final String[] titles = {"Worst Scores by Scr", "Best Scores by Scr"};
    final URL queryFile;
    final URL svgTemplate;
    final Path outputDir;
    private String slackToken = "";
    private String slackChannel = "";

    /**
     * Create an instance to generate Markdown and SVG tables
     * 
     * @param queryFile the query to run on Deephaven
     * @param svgTemplate the SVG template to apply table results to
     * @param outputDir the output directory to place the SVG and Markdown files
     */
    public PublishNotification(URL queryFile, URL svgTemplate, Path outputDir) {
        this.queryFile = queryFile;
        this.svgTemplate = svgTemplate;
        this.outputDir = outputDir;
    }

    /**
     * Generate the tables and publish to Slack
     */
    public void publish() {
        var query = Filer.getURLText(queryFile);
        var svgTemp = new String[] {Filer.getURLText(svgTemplate)};
        Bench api = Bench.create("# Publish Notification");
        try {
            api.setName("# Publish");
            slackChannel = api.property("slack.channel", "");
            slackToken = api.property("slack.token", "");
            if (slackChannel.isBlank() || slackToken.isBlank()) {
                System.out.println("-- Slack properties are not defined. Skipping query notification --");
                return;
            }
            System.out.println("-- Running notification queries --");
            var aquery = api.query(query);
            aquery.fetchAfter("platform_details", table -> {
                svgTemp[0] = updatePlatformDetails(table, svgTemp[0]);
            });
            for (int i = 0; i < tables.length; i++) {
                String tableName = tables[i], title = titles[i];
                aquery.fetchAfter(tableName + "_small", table -> {
                    generateMarkdown(table, title, outputDir, tableName + ".md");
                });
                aquery.fetchAfter(tableName + "_large", table -> {
                    generateSvg(table, svgTemp[0], outputDir, tableName + ".svg");
                });
            }
            aquery.execute();
        } finally {
            api.close();
        }

        publishToSlack(outputDir);
    }

    void publishToSlack(Path outDir) {
        var payload = getSlackPayload(outDir);
        try {
            System.out.println("-- Pushing notification to Slack --");
            URL url = new URI("https://slack.com/api/chat.postMessage").toURL();
            var c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + slackToken);
            try (OutputStream stream = c.getOutputStream()) {
                stream.write(payload.getBytes(StandardCharsets.UTF_8));
            }
            var in = (c.getResponseCode() < 400) ? c.getInputStream() : c.getErrorStream();
            var body = (in == null) ? "" : new String(in.readAllBytes(), StandardCharsets.UTF_8);
            System.out.println("-- Slack Response: " + c.getResponseCode() + " " + body + " --");
            c.disconnect();
        } catch (Exception ex) {
            System.out.println("Failed to Post to channel: " + slackChannel);
        }
    }

    String getSlackPayload(Path outDir) {
        var message = "Nightly Benchmark Changes " +
                "[(Benchmark Tables)](https://controller.try-dh.demo.community.deephaven.io/get_ide)\n";

        for (String table : tables) {
            message += "\n" + Filer.getFileText(outDir.resolve(table + ".md")) + "\n";
        }

        var payload = """
        {"channel": "${channel}", "unfurl_links": false, "unfurl_media": false, \
        "text": "Nightly Benchmark Changes", "blocks": [{"type": "markdown", "text": "${msg}"}]}""";
        payload = payload.replace("${channel}", toJsonString(slackChannel));
        return payload.replace("${msg}", toJsonString(message));
    }

    void generateMarkdown(ResultTable table, String title, Path outDir, String fileName) {
        var columnNames = table.getColumnNames();
        var headers = new ArrayList<Object>(columnNames);
        headers.set(0, headers.get(0) + " (" + title + ")");
        var md = new StringBuilder(getMarkdownRow(headers));
        md.append("\n|").append(":---|").append("---:|".repeat(columnNames.size() - 1));
        for (int i = 0, n = table.getRowCount(); i < n; i++) {
            md.append('\n').append(getMarkdownRow(table.getRow(i, columnNames)));
        }
        Filer.putFileText(outDir.resolve(fileName), md.toString());
    }

    private String getMarkdownRow(List<?> values) {
        var str = "|";
        for (Object value : values) {
            str += ' ' + String.valueOf(value).replaceAll("([\\\\|*_`~])", "\\\\$1") + " |";
        }
        return str;
    }

    static String toJsonString(String text) {
        var str = new StringBuilder();
        for (char c : text.toCharArray()) {
            switch (c) {
                case '"' -> str.append("\\\"");
                case '\\' -> str.append("\\\\");
                case '\n' -> str.append("\\n");
                case '\r' -> str.append("\\r");
                case '\t' -> str.append("\\t");
                default -> str.append((c < 0x20) ? String.format("\\u%04x", (int) c) : String.valueOf(c));
            }
        }
        return str.toString();
    }

    String updatePlatformDetails(ResultTable platform, String svgTemplate) {
        var s = svgTemplate;
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        s = s.replace("${run_date}", dtf.format(LocalDateTime.now()));
        s = s.replace("${os_name}", "Ubuntu 22.04.1 LTS".toLowerCase().replace(" ", "-"));
        // s = s.replace("${benchmark_count}", "" + benchmarks.size());
        s = replaceDetail("${dh_threads}", s, platform, "deephaven-engine", "available.processors");
        s = replaceDetail("${dh_heap}", s, platform, "deephaven-engine", "java.max.memory");
        return s;
    }

    void generateSvg(ResultTable table, String svgTemplate, Path outDir, String fileName) {
        var s = svgTemplate;
        var columnNames = table.getColumnNames();
        s = s.replace("${HEADER}", getRow("th", columnNames.toArray()));
        s = s.replace("${ROWS}", getRows("td", table, columnNames));
        Filer.putFileText(outDir.resolve(fileName), s);
    }

    private String replaceDetail(String var, String temp, ResultTable table, String origin, String name) {
        ResultTable t = table.findRows("origin", origin).findRows("name", name);
        return (t.getRowCount() < 1) ? temp : temp.replace(var, t.getValue(0, "value").toString());
    }

    private String getRows(String colMarkupElem, ResultTable table, List<String> columnNames) {
        var str = "";
        for (int i = 0, n = table.getRowCount(); i < n; i++) {
            if (i > 0)
                str += "\n" + rowIndent;
            str += getRow(colMarkupElem, table.getRow(i, columnNames).toArray());
        }
        return str;
    }

    private String getRow(String colMarkupElem, Object... values) {
        var str = "<tr>";
        for (Object value : values) {
            str += "<" + colMarkupElem + '>' + value + "</" + colMarkupElem + '>';
        }
        return str += "</tr>";
    }

}
