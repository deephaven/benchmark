/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.util.stream.IntStream;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Numbers;

/**
 * Generates an SVG from a template that contains variables (e.g. {@code ${op_duration}}) referencing benchmark data
 * from the Benchmark Result.
 * <p/>
 * 
 * @see benchmark-summary.template.svg
 */
class SvgSummary {
    final String varRegex = "(\\$\\{[^}]+\\})";
    final String subsRegex = "^.*" + String.join(".*", varRegex, varRegex, varRegex, varRegex) + ".*$";
    final Map<String, Benchmark> benchmarks;
    final String svgTemplate;
    final Path outputDir;
    final Path svgFile;

    /**
     * Configure this generator to correlate benchmark data with a summary template
     * 
     * @param benchmarkCsv the benchmark result data referenced by the template
     * @param svgTemplate the template containing references to the benchmark data
     * @param outputDir the output dir
     */
    SvgSummary(URL benchmarkCsv, URL svgTemplate, Path svgFile) {
        this.benchmarks = readBenchmarks(benchmarkCsv);
        this.svgTemplate = Filer.getURLText(svgTemplate);
        this.outputDir = svgFile.getParent();
        this.svgFile = svgFile;
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
                var benchmarkDescr = toVariableName(subs[0]);
                String[] benchmark = benchmarkDescr.split("=>");
                if (benchmark.length != 2)
                    throw new RuntimeException(
                            "Benchmark label must be of the form ${benchmark_name=>label}. Found: " + line.trim());
                line = replaceBenchVars(line, subs, benchmark[0].trim(), benchmark[1].trim());
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

    String replaceBenchVars(String line, String[] subs, String benchName, String benchDescr) {
        var benchmark = benchmarks.get(benchName);
        if (benchmark == null)
            return line;
        line = line.replace(subs[0], benchDescr);
        for (int i = 1; i < subs.length; i++) {
            var val = benchmark.getValue(toVariableName(subs[i]));
            line = line.replace(subs[i], Numbers.formatNumber(val));
        }
        return line;
    }

    private String replaceRunDate(String line) {
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        return line.replace("${run_date}", dtf.format(LocalDateTime.now()));
    }

    // Note: Platform variables are hardcoded for now.
    private String replacePlatformVars(String line) {
        line = line.replace("${dh_threads}", "16");
        line = line.replace("${dh_heap}", "24G".toLowerCase());
        return line.replace("${os_name}", "Ubuntu 22.04.1 LTS".toLowerCase());
    }

    private void println(StringBuilder str, String line) {
        line = replaceRunDate(line);
        line = replacePlatformVars(line);
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