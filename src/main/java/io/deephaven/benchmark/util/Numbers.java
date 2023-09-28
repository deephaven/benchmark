/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.text.DecimalFormat;

/**
 * Provide help with commonly used number parsing
 */
public class Numbers {
    static final DecimalFormat decimalFormat = new DecimalFormat("#,##0.000");
    static final DecimalFormat integralFormat = new DecimalFormat("#,##0");

    /**
     * Get a <code>Number</code> for the given value. If it is already a number, return it, otherwise attempt to parse
     * it as a <code>Long</code> or <code>Double</code> depending on whether or not it has a decimal.
     * 
     * @param val value to parse
     * @return a <code>Number</code> for the given value or null
     */
    static public Number parseNumber(Object val) {
        if (val == null)
            return null;
        if (val instanceof Number)
            return (Number) val;
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException ex) {
        }
        try {
            return Double.parseDouble(val.toString());
        } catch (Exception ex) {
            throw new RuntimeException("Bad number value: " + val);
        }
    }

    /**
     * Apply and pre-defined formatting to a given number to produce a string
     * 
     * @param val the value to format
     * @return the formatted value
     */
    static public String formatNumber(Object val) {
        if (val == null)
            return null;
        var num = parseNumber(val);
        if (num instanceof Double || num instanceof Float) {
            return decimalFormat.format(((Number) num).doubleValue());
        } else if (num instanceof Long || num instanceof Integer || num instanceof Short || num instanceof Byte) {
            return integralFormat.format(((Number) num).longValue());
        } else {
            return val.toString();
        }
    }

    /**
     * Format a number value according to the given pattern
     * 
     * @param val a number value as a String or Number
     * @param pattern a DecimalFormat-supported pattern
     * @return the formatted number or null if none was given
     */
    static public String formatNumber(Object val, String pattern) {
        if (val == null)
            return null;
        return new DecimalFormat(pattern).format(parseNumber(val));
    }

    /**
     * Format a given byte count into Gigabytes. ex. 1g, 200g
     * 
     * @param bytes the byte count to convert
     * @return a string of the form 1g
     */
    static public String formatBytesToGigs(Object val) {
        if (val == null)
            return null;
        long bytes = parseNumber(val).longValue();
        return "" + (bytes / 1024 / 1024 / 1024) + "g";
    }

}
