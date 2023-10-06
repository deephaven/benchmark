/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Filer;

/**
 * Generate benchmark tables for posting to Slack. Produces tables in CSV and SVG form. These tables are different from
 * other auto-generated tables in that they pull data exclusively from GCloud.
 */
public class PublishNotification {
    static final String rowIndent = " ".repeat(10);
    final String[] tables = {"nightly_worst_rate_change"};
    final URL queryFile;
    final URL svgTemplate;
    final Path outputDir;
    private String slackToken = "";
    private String slackChannel = "";

    /**
     * Create an instance to generate CSV and SVG tables
     * 
     * @param queryFile the query to run on Deephaven
     * @param svgTemplate the SVG template to apply table results to
     * @param outputDir the output directory to place the SVG and CSV files
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
        api.setName("# Publish");
        slackChannel = api.property("slack.channel", "");
        slackToken = api.property("slack.token", "");
        if (slackChannel.isBlank() || slackToken.isBlank()) {
            api.close();
            System.out.println("-- Slack properties is not defined, skipping query notification --");
            return;
        }
        System.out.println("-- Running notification queries --");
        var aquery = api.query(query);
        aquery.fetchAfter("platform_details", table -> {
            svgTemp[0] = updatePlatformDetails(table, svgTemp[0]);
        });
        for (String tableName : tables) {
            aquery.fetchAfter(tableName, table -> {
                generateSvg(table, svgTemp[0], outputDir, tableName + ".svg");
                generateCsv(table, outputDir, tableName + ".csv");
            });
        }
        aquery.execute();
        api.close();

        publishToSlack(outputDir);
    }

    void publishToSlack(Path outDir) {
        var message = "Nightly Benchmark Changes Since Last Release\n";
        for (String table : tables) {
            message += "```" + Filer.getFileText(outDir.resolve(table + ".csv")) + "```";
        }

        var payload = """
        {"channel": "${channel}", "text": "${msg}"}
        """;
        payload = payload.replace("${channel}", slackChannel);
        payload = payload.replace("${msg}", message);
        try {
            System.out.println("-- Pushing notification to Slack --");
            URL url = new URL("https://slack.com/api/chat.postMessage");
            var c = (HttpURLConnection) url.openConnection();
            c.setRequestMethod("POST");
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", "application/json");
            c.setRequestProperty("Accept", "application/json");
            c.setRequestProperty("Authorization", "Bearer " + slackToken);
            byte[] out = payload.getBytes(StandardCharsets.UTF_8);
            OutputStream stream = c.getOutputStream();
            stream.write(out);
            System.out.println("-- Slack Response: " + c.getResponseCode() + " " + c.getResponseMessage() + "--");
            c.disconnect();
        } catch (Exception ex) {
            System.out.println("Failed to Post to channel: " + slackChannel);
        }
    }

    void generateCsv(ResultTable table, Path outDir, String fileName) {
        var s = table.toCsv("  ", "LR");
        Filer.putFileText(outDir.resolve(fileName), s);
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
