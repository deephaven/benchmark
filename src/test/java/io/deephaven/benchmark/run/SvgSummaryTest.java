/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.Before;
import org.junit.jupiter.api.Test;
import io.deephaven.benchmark.util.Filer;

public class SvgSummaryTest {

    @Before
    public void setup() throws Exception {

    }

    @Test
    public void summarize() throws Exception {
        var template = getClass().getResource("test-summary.template.svg");
        var csv = getClass().getResource("test-benchmark-results.csv");
        var svg = Paths.get(template.toURI()).resolveSibling("benchmark-summary.svg");
        var summary = new SvgSummary(csv, template, svg);
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
                    <tr><td>Avg By Row1</td><td>12.068</td><td>14,915,478</td><td>180,000,000</td></tr>
                    <tr><td>Median By Row2</td><td>17.963</td><td>2,226,799</td><td>40,000,000</td></tr>
                    <tr><td>Kafka Read Row 3</td><td>11.860</td><td>210,792</td><td>2,500,000</td></tr>
                  </tbody>
                  <tfoot><tr><td colspan="4">* threads=16 heap=24g os=ubuntu 22.04.1 lts</td></tr></tfoot>
                </table>
              </div>
            </foreignObject>
          </svg>
          """.replace("\r", "").trim().replaceAll("[2-9]{4}[-][1-12]{1,2}[-][1-12]{1,2}", "2023-7-18"),
                Filer.getFileText(summary.svgFile));
    }

}
