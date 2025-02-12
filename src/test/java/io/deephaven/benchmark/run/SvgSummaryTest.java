/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;
import io.deephaven.benchmark.util.Filer;

public class SvgSummaryTest {

    @Test
    public void summarize() throws Exception {
        var template = getClass().getResource("test-summary.template.svg");
        var benchmarkCsv = getClass().getResource("test-benchmark-results.csv");
        var platformCsv = getClass().getResource("test-platform-results.csv");
        var svg = Paths.get(template.toURI()).resolveSibling("benchmark-summary.svg");
        var summary = new SvgSummary(platformCsv, benchmarkCsv, template, svg);
        Files.deleteIfExists(summary.svgFile);
        summary.summarize();
        assertEquals("""
          <svg viewBox="0 0 170 95" xmlns="http://www.w3.org/2000/svg">
            <style>
              div {
                color: #f0f0ee;
                font: 3.0px fira sans,sans-serif;
              }
            </style>
            <foreignObject x="0" y="0" width="100%" height="100%">
              <div xmlns="http://www.w3.org/1999/xhtml">
                <table><tr><th>Deephaven</th><th>Summary</th><th>2023-07-18</th></tr></table>
                <table cellspacing="0">
                  <thead>
                    <tr><th>Benchmark</th><th>Op Duration</th><th>Op Rate</th><th>Row Count</th></tr>
                  </thead>
                  <tbody>
                    <tr><td>Avg By Row1</td><td>14,915,478</td><td>9,609,994</td></tr>
                    <tr><td>Median By Row2</td><td>2,309,348</td><td>2,226,799</td></tr>
                    <tr><td>Avg By Row1</td><td>14,915,478</td><td>9,609,994</td></tr>
                    <tr><td>Median By Row2</td><td>2,309,348</td><td>2,226,799</td></tr>
                  </tbody>
                  <tfoot><tr><td colspan="3">* threads=16 heap=24g os=ubuntu-22.04.5-lts benchmark-count=5</td></tr></tfoot>
                </table>
              </div>
            </foreignObject>
          </svg>
          """.replace("\r", "").trim(),
                Filer.getFileText(summary.svgFile).replaceAll("[0-9]{4}[-][0-9]{2}[-][0-9]{2}", "2023-07-18"));
    }

}
