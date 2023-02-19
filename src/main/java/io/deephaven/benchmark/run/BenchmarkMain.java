/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.nio.file.Paths;
import org.junit.platform.console.ConsoleLauncher;
import io.deephaven.benchmark.api.Bench;

public class BenchmarkMain {
    static public void main(String[] args) {
        System.exit(main1(args));
    }

    static public int main1(String[] args) {
        setSystemProperties();
        int exitCode = ConsoleLauncher.execute(System.out, System.err, args).getExitCode();
        if (exitCode == 0) {
            new ResultSummary(Paths.get(Bench.rootOutputDir)).summarize();
        }
        return exitCode;
    }

    // Set system properties for running from the command line
    static void setSystemProperties() {
        System.setProperty("timestamp.test.results", "true");
    }

}
