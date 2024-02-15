/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

public class NumbersTest {
    @Test
    public void parseNumber() {
        assertNull(Numbers.parseNumber(null), "Should be null");
        assertEquals((short) 10, Numbers.parseNumber((short) 10), "Should return the same type as passed");
        assertEquals((double) 10.01, Numbers.parseNumber("10.01"), "Should convert to double");
        assertEquals((long) 11, Numbers.parseNumber("11"), "Should convert to long");
    }

    @Test
    public void formatNumber() {
        assertNull(Numbers.formatNumber(null), "Should be null");
        assertEquals("1,234.568", Numbers.formatNumber(1234.5679), "Should have 3 decimal places w/ commas");
        assertEquals("1,234.568", Numbers.formatNumber("1234.5679"), "Should have 3 decimal places w/ commas");
        assertEquals("0.568", Numbers.formatNumber(0.5679), "Should have 3 decimal places w/ commas");
        assertEquals("0.568", Numbers.formatNumber("0.5678"), "Should have 3 decimal places w/ commas");
        assertEquals("1,234,567,890", Numbers.formatNumber(1234567890), "Should have commas");
        assertEquals("1,234,567,890", Numbers.formatNumber("1234567890"), "Should have commas");
        assertEquals("0", Numbers.formatNumber(0), "Should be 0");
        assertEquals("0", Numbers.formatNumber("0"), "Should be 0");
    }

    @Test
    public void formatBytesToGigs() {
        assertNull(Numbers.formatBytesToGigs(null), "Should be null");
        assertEquals("24g", Numbers.formatBytesToGigs(25769803776L));
    }

    @Test
    public void formatNumberPattern() {
        assertNull(Numbers.formatNumber(null, null), "Should be null");
        assertEquals("1,234.6", Numbers.formatNumber(1234.5679, "#,##0.0"), "Should have 1 decimal place w/ commas");
        assertEquals("1,235", Numbers.formatNumber(1234.5679, "#,##0"), "Should have 0 decimal places w/ commas");
        assertEquals("12.3%", Numbers.formatNumber(0.123, "0.0%"), "Should be converted to percent");
        assertEquals("123,000.0%", Numbers.formatNumber(1230, "#,##0.0%"), "Should be converted to percent");
    }

    @Test
    public void negate() {
        assertNull(Numbers.negate(null), "Should be null");
        assertEquals(Integer.valueOf(-10), Numbers.negate(Integer.valueOf(10)), "Wrong negated Integer value");
        assertEquals(Long.valueOf(-11), Numbers.negate(Long.valueOf(11)), "Wrong negated Long value");
        assertEquals(Short.valueOf((short) -12), Numbers.negate(Short.valueOf((short) 12)),
                "Wrong negated Short value");
        assertEquals(Byte.valueOf((byte) -13), Numbers.negate(Byte.valueOf((byte) 13)), "Wrong negated Byte value");
        assertEquals(Float.valueOf(-1.1f), Numbers.negate(Float.valueOf(1.1f)), "Wrong negated Float value");
        assertEquals(Double.valueOf(-1.2), Numbers.negate(Double.valueOf(1.2)), "Wrong negated Double value");
    }

    @Test
    public void isEven() {
        assertFalse(Numbers.isEven(null), "Should be null");
        assertTrue(Numbers.isEven(2), "Integer should be even");
        assertTrue(Numbers.isEven(0), "Integer should be even");
        assertFalse(Numbers.isEven(3), "Integer should be odd");
        assertTrue(Numbers.isEven(2.13), "Decimal should be even");
        assertFalse(Numbers.isEven(3.12), "Decimal should be even");
    }

}
