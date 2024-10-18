/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

// Basic log (worry about formatting later)
/**
 * Basic log mechanism (Really a placeholder for now)
 */
public class Log {
    /**
     * Log some info as a formmated set of values
     * 
     * @param formattedMsg a format accepted by <code>printf</code>
     * @param values values for the given message
     */
    static public void info(String formattedMsg, Object... values) {
        System.out.printf(formattedMsg, values);
        System.out.println();
    }
}
