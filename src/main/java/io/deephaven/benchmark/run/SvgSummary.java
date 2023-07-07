/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.IntStream;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Numbers;

class SvgSummary {
    final String varRegex = "(\\$\\{[^}]+\\})";
    final String subsRegex = "^.*" + String.join(".*", varRegex, varRegex, varRegex, varRegex) + ".*$";
    final Map<String, Benchmark> benchmarks;
    final String svgTemplate;
    final Path outputDir;
    final Path svgFile;

    SvgSummary(URL benchmarkCsv, URL svgTemplate, Path outputDir) {
        this.benchmarks = readBenchmarks(benchmarkCsv);
        this.svgTemplate = Filer.getURLText(svgTemplate);
        this.outputDir = outputDir;
        this.svgFile = outputDir.resolve("benchmark-summary.svg");
    }

    void summarize() {
        if (!Files.exists(outputDir)) {
            System.out.println("Skipping SVG summary because of missing output directory: " + outputDir);
            return;
        }

        var out = new StringBuilder();
        svgTemplate.lines().forEach(line -> {
            if (line.matches(subsRegex)) {
                String[] subs = line.replaceAll(subsRegex, "$1,$2,$3,$4").split(",");
                var benchmark = toVariableName(subs[0]);
                var success = appendBenchmarkVariables(out, line, subs, benchmark + " -Static");
                success = success | appendBenchmarkVariables(out, line, subs, benchmark + " -Inc");
                success = success | appendBenchmarkVariables(out, line, subs, benchmark);
                if (!success)
                    println(out, line);
            } else {
                println(out, line);
            }
        });
        Filer.putFileText(svgFile, out);
    }

    Map<String, Benchmark> readBenchmarks(URL csv) {
        var header = new HashMap<String, Integer>();
        var benchmarks = new HashMap<String, Benchmark>();
        var csvLines = Filer.getURLText(csv).lines().toList();
        for (int i = 0, n = csvLines.size(); i < n; i++) {
            String[] values = csvLines.get(i).split(",");
            if (i == 0) {
                IntStream.range(0, values.length).forEach(pos -> header.put(values[pos], pos));
            } else {
                var benchmark = new Benchmark(header, values);
                benchmarks.put(benchmark.getValue("benchmark_name"), benchmark);
            }
        } ;
        return benchmarks;
    }

    boolean appendBenchmarkVariables(StringBuilder out, String line, String[] subs, String benchmarkName) {
        var benchmark = benchmarks.get(benchmarkName);
        if (benchmark == null)
            return false;
        line = line.replace(subs[0], benchmarkName);
        for (int i = 1; i < subs.length; i++) {
            var val = benchmark.getValue(toVariableName(subs[i]));
            line = line.replace(subs[i], Numbers.formatNumber(val));
        }
        println(out, line);
        return true;
    }

    private void println(StringBuilder str, String line) {
        str.append(line).append('\n');
    }

    private String toVariableName(String curlyVar) {
        return curlyVar.replaceAll("^\\$\\{", "").replaceAll("\\}$", "");
    }

    record Benchmark(Map<String, Integer> header, String[] values) {
        String getValue(String colName) {
            Integer index = header.get(colName);
            if (index == null)
                throw new RuntimeException("Undefined benchmark column name: " + colName);
            return values[index].trim();
        }
    }

}
