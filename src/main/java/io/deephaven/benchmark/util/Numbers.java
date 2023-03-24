/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

/**
 * Provide help with commonly used number parsing
 */
public class Numbers {

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

}
