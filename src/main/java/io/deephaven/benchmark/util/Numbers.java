/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
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

    /**
     * Return the negative of the given number instance. If <code>val</code> is null or zero, return.
     * 
     * @param val a number
     * @return the negated number or null if <code>val</code> was null
     */
    static public Number negate(Object val) {
        if (val instanceof Number && ((Number) val).doubleValue() == 0.0)
            return (Number) val;
        return switch (val) {
            case Integer v -> -v.intValue();
            case Float v -> -v.floatValue();
            case Long v -> -v.longValue();
            case Double v -> -v.doubleValue();
            case Short v -> (short) (-v.shortValue());
            case Byte v -> (byte) (-v.byteValue());
            case null -> null;
            default -> throw new RuntimeException("Unsupported Type: " + val.getClass().getSimpleName());
        };
    }

    /**
     * Determine whether the given number is even or odd.
     * <p/>
     * Note: While this method accepts any Number, even or odd for a decimal will be determined by the integral portion.
     * 
     * @param val a number
     * @return true if an even number, otherwise false
     */
    static public boolean isEven(Object val) {
        return switch (val) {
            case Number v -> v.longValue() % 2 == 0;
            case null -> false;
            default -> throw new RuntimeException("Unsupported Type: " + val.getClass().getSimpleName());
        };
    }

    /**
     * Subtract the number in the given string from the given max offset and replace it in the string.
     * 
     * @param value the string containing a number
     * @param maxOffset the uppermost possible number the string may contain
     * @return a string with the number replaced by a number offset from the maximum
     */
    static public Object offsetInString(Object value, long offset, long size) {
        var str = value.toString().replaceAll("([^0-9]*)([0-9]+)([^0-9]*)", "$1,,,$2,,,$3");
        var split = str.split(",,,", -1);
        if (split.length != 3)
            return value;
        var num = parseNumber(split[1]).longValue();
        num = offset + size - num + offset;
        return split[0] + num + split[2];
    }

}
