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
        var outDir = Paths.get(template.toURI()).getParent();
        var summary = new SvgSummary(csv, template, outDir);
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
                <table cellspacing="0">
                  <thead>
                    <tr><th>Benchmark</th><th>Op Duration</th><th>Op Rate</th><th>Row Count</th></tr>
                  </thead>
                  <tbody>
                    <tr><td>AvgBy- 2 Groups 160K Unique Combos Int -Static</td><td>12.068</td><td>14,915,478</td><td>180,000,000</td></tr>
                    <tr><td>AvgBy- 2 Groups 160K Unique Combos Int -Inc</td><td>12.487</td><td>9,609,994</td><td>120,000,000</td></tr>
                    <tr><td>MedianBy- 2 Group 160K Unique Combos Float -Static</td><td>16.602</td><td>2,409,348</td><td>40,000,000</td></tr>
                    <tr><td>MedianBy- 2 Group 160K Unique Combos Float -Inc</td><td>17.963</td><td>2,226,799</td><td>40,000,000</td></tr>
                    <tr><td>NoOp- 20 Double Cols JSON Append</td><td>11.860</td><td>210,792</td><td>2,500,000</td></tr>
                  </tbody>
                </table>
              </div>
            </foreignObject>
          </svg>
                """.replace("\r", "").trim(), Filer.getFileText(summary.svgFile));
    }

}
