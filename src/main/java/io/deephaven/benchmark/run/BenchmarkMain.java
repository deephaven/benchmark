/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.platform.console.ConsoleLauncher;
import io.deephaven.benchmark.api.Bench;

/**
 * Main class for running the benchmark framework from the deephaven-benchmark artifact. This wraps the JUnit standalone
 * console launcher.
 * <p/>
 * ex. java -Dbenchmark.profile=my-benchmark.properties -jar deephaven-benchmark-1.0-SNAPSHOT.jar -cp my-tests.jar -p
 * my.tests
 * <p/>
 * In addition to running benchmark tests using the console launcher, this class creates a
 * <code>benchmark-summary-results.csv</code> that is a merge of any per-run results files that match
 * <code>results/run-[A-za-z0-9]+/benchmark-results.csv</code> relative to the working directory
 */
public class BenchmarkMain {
    /**
     * Main method for executing the benchmark framework from the command line. Accepts the same arguments as the JUnit
     * standalone console launcher
     * 
     * @param args standalone console launcher arguments
     */
    static public void main(String[] args) {
        System.exit(main1(args));
    }

    static int main1(String[] args) {
        setSystemProperties();
        int exitCode = ConsoleLauncher.execute(System.out, System.err, args).getExitCode();
        if (exitCode == 0) {
            Path d = Paths.get(Bench.rootOutputDir);
            URL platformCsv = new ResultSummary(d, "platform-summary-results.csv", Bench.platformFileName).summarize();
            URL benchmarkCsv = new ResultSummary(d, "benchmark-summary-results.csv", Bench.resultFileName).summarize();
            toSummarySvg(platformCsv, benchmarkCsv, "standard", d, "nightly");
            toSummarySvg(platformCsv, benchmarkCsv, "standard", d, "release");
            toSummarySvg(platformCsv, benchmarkCsv, "compare", d, "compare");
        }
        return exitCode;
    }

    static void toSummarySvg(URL platformCsv, URL benchCsv, String tmpPrefix, Path outputDir, String outputPrefix) {
        URL svgTemplate = BenchmarkMain.class.getResource("profile/" + tmpPrefix + "-benchmark-summary.template.svg");
        new SvgSummary(platformCsv, benchCsv, svgTemplate, outputDir.resolve(outputPrefix + "-benchmark-summary.svg"))
                .summarize();
    }

    // Set system properties for running from the command line
    static void setSystemProperties() {
        System.setProperty("timestamp.test.results", "true");
    }

}
