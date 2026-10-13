/* Copyright (c) 2022-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.run;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Utility methods for working with the JUnit Console Launcher
 */
public class ConsoleLauncherUtil {
    static final Set<String> commands = Set.of("execute", "discover", "engines");

    /**
     * Prepend the <code>execute</code> subcommand to the console launcher args if no subcommand is given. The console
     * launcher requires a subcommand (since JUnit 6), but the Benchmark scripts and users supply only the options.
     * 
     * @param args console launcher args
     * @return console launcher args beginning with a subcommand
     */
    public static String[] addDefaultCommand(String[] args) {
        if (args.length > 0 && commands.contains(args[0]))
            return args;
        var newArgs = new String[args.length + 1];
        newArgs[0] = "execute";
        System.arraycopy(args, 0, newArgs, 1, args.length);
        return newArgs;
    }

    /**
     * Format console launcher args to expand wildcard test patterns to regular expressions. This is not intended to
     * cover every wildcard that can be passed to the Console Launcher, just the ones used by the Benchmark scripts.
     * 
     * @param args console launcher args
     * @return formatted console launcher args
     */
    public static String[] formatConsoleWildcards(String[] args) {
        if (args.length == 0)
            return args;
        var newArgs = Arrays.copyOf(args, args.length);
        for (int i = 0, n = newArgs.length - 1; i < n; i++) {
            if (!newArgs[i].equals("-n"))
                continue;
            int p = i + 1;
            newArgs[p] = "^.*[.]" + formatRegex(newArgs[p]) + "Test.*$";
        }
        return newArgs;
    }

    static String formatRegex(String wildcardList) {
        var r = Arrays.stream(wildcardList.split("\\s*[,]\\s*")).map(w -> w.replace("*", ".*"))
                .collect(Collectors.joining("|"));
        return "(" + r + ")";
    }

}
