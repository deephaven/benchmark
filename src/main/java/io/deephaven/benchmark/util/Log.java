/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

// Basic log (worry about formatting later)
public class Log {
    static public void info(String formattedMsg, Object... values) {
        System.out.printf(formattedMsg, values);
        System.out.println();
    }
}
