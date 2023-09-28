/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.time.*;
import java.time.format.DateTimeFormatter;

/**
 * Provide help with commonly used date parsing. Imply standard datetime formats and conversion for different types
 * (e.g. Number, Instant, String)
 */
public class Dates {

    /**
     * Get an {@code Instant} out of the given value. Supports converting from Epoch Instant, Epoch Millis, and ISO
     * DateTime String,
     * 
     * @param val a Number, Instant or String
     * @return an Instant on the input
     */
    static public Instant parseInstant(Object val) {
        if (val instanceof Instant)
            return (Instant) val;
        if (val instanceof Number) {
            return Instant.ofEpochMilli(((Number) val).longValue());
        }
        var v = val.toString();
        var instant = ZonedDateTime.parse(v).toInstant();
        if (instant == null)
            throw new RuntimeException("Failed to parse datetime: " + val);
        return instant;
    }

    /**
     * Format a given value to the given {@code DateTimeFormatter} pattern. The give value will be first converted to an
     * Instant first with {@code parseInstant()} and then formatted.
     * 
     * @param val a Number, Instant or String
     * @param pattern a DateTimeFormatter-supported pattern
     * @return a formatted string or null if no value is given
     */
    static public String formatDate(Object val, String pattern) {
        if (val == null || pattern == null)
            return null;
        return DateTimeFormatter.ofPattern(pattern).withZone(ZoneId.of("UTC")).format(parseInstant(val));
    }

}
