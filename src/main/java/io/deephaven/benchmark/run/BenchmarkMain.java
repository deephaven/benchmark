/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.nio.file.Paths;
import java.util.*;
import org.junit.platform.console.ConsoleLauncher;
import io.deephaven.benchmark.api.Bench;

public class BenchmarkMain {
    static public void main(String[] args) {
        setSystemProperties();
        int exitCode = ConsoleLauncher.execute(System.out, System.err, args).getExitCode();
        new ResultSummary(Paths.get(Bench.rootOutputDir)).summarize();
        System.exit(exitCode);
    }

    // Set system properties for running from the command line
    static void setSystemProperties() {
        System.setProperty("timestamp.test.results", "true");
    }

    static void addDefault(List<String> args, String shortSwitch, String longSwitch, String defaultValue) {
        if (foundShortSwitch(args, shortSwitch) || foundLongSwitch(args, longSwitch))
            return;
        args.add(shortSwitch);
        args.add(defaultValue);
    }

    static boolean foundShortSwitch(List<String> args, String shortSwitch) {
        return (args.indexOf(shortSwitch) >= 0);
    }

    static boolean foundLongSwitch(List<String> args, String longSwitch) {
        return args.stream().anyMatch(a -> a.startsWith(longSwitch));
    }

}
