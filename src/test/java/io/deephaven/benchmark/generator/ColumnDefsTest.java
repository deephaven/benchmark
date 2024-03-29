/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import java.util.stream.*;
import org.junit.jupiter.api.*;

public class ColumnDefsTest {

    @Test
    public void add() {
        ColumnDefs columnDefs = new ColumnDefs()
                .add("symbol", "string", "ABC[1-11]")
                .add("price", "float", "[100-105]")
                .add("priceAgain", "int", "[100-105]");
        columnDefs.setDefaultDistribution("incremental");

        assertEquals(3, columnDefs.columns.size(), "Wrong def count");

        assertEquals("string", columnDefs.columns.get(0).type(), "Wrong field count");
        assertEquals("symbol", columnDefs.columns.get(0).name(), "Wrong field name");
        assertEquals("StringMaker", columnDefs.columns.get(0).maker().getClass().getSimpleName(), "Wrong field maker");
        assertEquals("ABC2", columnDefs.nextValue(0, 1, 105), "Wrong field next");

        assertEquals("float", columnDefs.columns.get(1).type(), "Wrong field count");
        assertEquals("price", columnDefs.columns.get(1).name(), "Wrong field name");
        assertEquals("FloatMaker", columnDefs.columns.get(1).maker().getClass().getSimpleName(), "Wrong field maker");
        assertEquals("[100.0, 101.0, 102.0, 103.0, 104.0, 105.0]", columnDefs.columns.get(1).maker().cache.toString(),
                "Wrong field maker");
        assertEquals(101.0f, columnDefs.nextValue(1, 1, 105), "Wrong field next");

        assertEquals("[100, 101, 102, 103, 104, 105]", columnDefs.columns.get(2).maker().cache.toString(),
                "Wrong field maker");
        assertEquals(105, columnDefs.nextValue(2, 5, 105), "Wrong field next");
        assertEquals(100, columnDefs.nextValue(2, 0, 105), "Wrong field next");
    }

    @Test
    public void add_Literals() {
        ColumnDefs columnDefs = new ColumnDefs()
                .add("col1", "string", "11")
                .add("col2", "long", "12")
                .add("col3", "int", "13")
                .add("col4", "double", "14")
                .add("col5", "float", "15")
                .add("col6", "timestamp-millis", "16");

        for (int i = 0; i < 10; i++) {
            assertEquals("11", columnDefs.nextValue(0, i, 1));
            assertEquals((Long) 12L, columnDefs.nextValue(1, i, 1));
            assertEquals((Integer) 13, columnDefs.nextValue(2, i, 1));
            assertEquals((Double) 14D, columnDefs.nextValue(3, i, 1));
            assertEquals((Float) 15F, columnDefs.nextValue(4, i, 1));
            assertEquals((Long) 16L, columnDefs.nextValue(5, i, 1));
        }
    }

    @Test
    public void getQuotedColumns() {
        ColumnDefs columnDefs = new ColumnDefs()
                .add("symbol", "string", "ABC[1-11]")
                .add("price", "float", "[100-105]")
                .add("priceAgain", "int", "[100-105]");

        assertEquals("\"symbol\",\"price\",\"priceAgain\"", columnDefs.getQuotedColumns(), "Wrong field next");
    }

    @Test
    public void getMaxValueCount() {
        ColumnDefs columnDefs = new ColumnDefs()
                .add("symbol", "string", "ABC[1-10]")
                .add("price", "float", "[100-105]")
                .add("priceAgain", "int", "[100-105]");

        assertEquals(10, columnDefs.getMaxValueCount(), "Wrong row count");
    }

    @Test
    public void describe() {
        ColumnDefs columnDefs = new ColumnDefs()
                .add("symbol", "string", "ABC[1-10]")
                .add("price", "float", "[100-105]")
                .add("priceAgain", "int", "[100-105]", "linearConv");

        assertEquals("""
                name,type,values,distribution
                symbol,string,ABC[1-10],random
                price,float,[100-105],random
                priceAgain,int,[100-105],linearconv
                """,
                columnDefs.describe(), "Wrong toString");

        columnDefs = new ColumnDefs()
                .add("symbol", "string", "ABC[1-10]")
                .add("price", "float", "[100-105]")
                .add("priceAgain", "int", "[100-105]", "linearConv");
        columnDefs.setDefaultDistribution("incremental");

        assertEquals("""
                name,type,values,distribution
                symbol,string,ABC[1-10],incremental
                price,float,[100-105],incremental
                priceAgain,int,[100-105],linearconv
                """,
                columnDefs.describe(), "Wrong toString");
    }

    @Test
    public void nextValue_Incremental() {
        ColumnDefs columnDefs = new ColumnDefs(5).add("v", "string", "s[1-7]");
        columnDefs.setDefaultDistribution("incremental");

        var vals = IntStream.range(0, 10).mapToObj(i -> columnDefs.nextValue(0, i, 10)).toList();
        assertEquals("[s1, s2, s3, s4, s5, s6, s7, s1, s2, s3]", vals.toString(), "Wrong generated sequence");

        Map<String, Set<Integer>> unique = new LinkedHashMap<>();

        IntStream.range(0, 21).mapToObj(i -> columnDefs.nextValue(0, i, 21)).forEach(v -> {
            unique.computeIfAbsent("" + v, ids -> new HashSet<>());
            unique.get("" + v).add(System.identityHashCode(v));
        });
        Set<String> occurrences = new LinkedHashSet<>();
        unique.forEach((k, v) -> occurrences.add(k + ':' + v.size()));

        assertEquals("[s1:1, s2:1, s3:1, s4:1, s5:1, s6:3, s7:3]", occurrences.toString(),
                "Cache objects (1-5) should not be repeated");
    }

    @Test
    public void nextValue_Random() {
        var columnDefs1 = new ColumnDefs(5).add("v", "string", "s[1-7]");
        columnDefs1.setDefaultDistribution("random");

        var vals = IntStream.range(0, 10).mapToObj(i -> columnDefs1.nextValue(0, i, 10)).toList();
        assertEquals("[s6, s6, s1, s2, s1, s5, s7, s1, s6, s5]", vals.toString(), "Wrong generated sequence");

        var columnDefs2 = new ColumnDefs(5).add("v", "string", "s[1-7]");
        columnDefs1.setDefaultDistribution("random");

        Map<String, Set<Integer>> unique = new LinkedHashMap<>();

        IntStream.range(0, 30).mapToObj(i -> columnDefs2.nextValue(0, i, 30)).forEach(v -> {
            unique.computeIfAbsent("" + v, ids -> new HashSet<>());
            unique.get("" + v).add(System.identityHashCode(v));
        });
        Set<String> occurrences = new LinkedHashSet<>();
        unique.forEach((k, v) -> occurrences.add(k + ':' + v.size()));

        assertEquals("[s6:8, s1:1, s2:1, s5:1, s7:2, s3:1, s4:1]", occurrences.toString(),
                "Cache objects (1-5) should not be repeated");
    }

    @Test
    public void nextValue_LinearConv() {
        var columnDefs1 = new ColumnDefs(5).add("v", "string", "s[2-8]", "linearConv");

        var vals = IntStream.range(0, 10).mapToObj(i -> columnDefs1.nextValue(0, i, 10)).toList();
        assertEquals("[s2, s2, s3, s4, s4, s5, s6, s6, s7, s8]", vals.toString(), "Wrong generated sequence");

        var columnDefs2 = new ColumnDefs(5).add("v", "string", "s[1-7]", "linearConv");

        Map<String, Set<Integer>> unique = new LinkedHashMap<>();

        IntStream.range(0, 21).mapToObj(i -> columnDefs2.nextValue(0, i, 21)).forEach(v -> {
            unique.computeIfAbsent("" + v, ids -> new HashSet<>());
            unique.get("" + v).add(System.identityHashCode(v));
        });
        Set<String> occurrences = new LinkedHashSet<>();
        unique.forEach((k, v) -> occurrences.add(k + ':' + v.size()));

        assertEquals("[s1:1, s2:1, s3:1, s4:1, s5:1, s6:3, s7:3]", occurrences.toString(),
                "Cache objects (1-5) should not be repeated");
    }

}
