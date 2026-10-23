/* Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.util.regex.Pattern;

/**
 * Provide help with validating names
 */
public class Names {
    static final int maxBenchmarkBaseLength = 58;
    static final Pattern benchmarkNamePattern = Pattern.compile("[A-Za-z0-9][A-Za-z0-9()\\-]*( [A-Za-z0-9()\\-]+)*");
    static final Pattern benchmarkModeSuffix = Pattern.compile(" -(Static|Inc)$");

    /**
     * Throw an error if the given benchmark name is not valid. Names end up in csv results, query strings, dashboards,
     * Slack tables and file names (e.g. JFR recordings), so only allow the characters benchmarks already use. Names
     * starting with "#" are internal sessions that are not recorded as benchmarks, so they are not checked.
     * <p>
     * Rules:
     * <ul>
     * <li>Characters: letters, digits, space, hyphen, parentheses</li>
     * <li>Starts with a letter or digit</li>
     * <li>Single spaces only, and no leading or trailing space</li>
     * <li>Base name (without the " -Static" or " -Inc" suffix) of at most 58 characters, which matches the Slack
     * notification table truncation in <code>publish.py</code></li>
     * </ul>
     * 
     * @param name the benchmark name to validate
     */
    static public void validateBenchmarkName(String name) {
        if (name == null || name.isBlank())
            throw new RuntimeException("No blank Benchmark names allowed");
        if (name.startsWith("#"))
            return;
        if (!benchmarkNamePattern.matcher(name).matches())
            throw new RuntimeException("Invalid Benchmark name '" + name + "': Use only letters, digits, hyphens, "
                    + "parentheses and single spaces, starting with a letter or digit");
        var base = benchmarkModeSuffix.matcher(name).replaceFirst("");
        if (base.length() > maxBenchmarkBaseLength)
            throw new RuntimeException("Benchmark name '" + base + "' is " + base.length()
                    + " characters, which is over the max of " + maxBenchmarkBaseLength);
    }

}
