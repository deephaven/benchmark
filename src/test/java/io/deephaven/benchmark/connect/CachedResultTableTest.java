/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.List;
import org.junit.jupiter.api.Test;

public class CachedResultTableTest {
    String csv = """
    RowPosition|     RowKey|    symbol|            AvgPrice|     Total|            RecCount
    ----------+----------+----------+--------------------+----------+--------------------
            0|         0|GIS       |                 5.0|      10.0|                   2
            1|         1|AAPL      |                 6.0|      10.0|                   2
            2|         2|MSFT      |                 7.0|      14.0|                   2
    """;

    String csv2 = """
    symbol, timestamp, ratio, ratio__TABLE_NUMBER_FORMAT, timestamp__TABLE_DATE_FORMAT
    GIS, 2023-09-08T09:37:07.001Z, 0.05, 0.0%, yyyy-MM-dd
    AAPL, 2023-09-08T09:37:07.002Z, 6, 0.0, yyyy-MM-dd
    MSFT, 2023-09-08T09:37:07.003Z, 0.07, 0.0%, yyyy-MM-dd
    """;

    @Test
    public void fromBarrageCsv() {
        var table = CachedResultTable.create(csv, "|");

        assertEquals("[RowPosition, RowKey, symbol, AvgPrice, Total, RecCount]", table.getColumnNames().toString(),
                "Wrong columns");
        assertEquals(3, table.getRowCount(), "Wrong row count");
        assertEquals("0", table.getValue(0, "RowPosition"), "Wrong row value");
        assertEquals("MSFT", table.getValue(2, "symbol"), "Wrong row value");
        assertEquals(18.0, table.getSum("AvgPrice"), "Wrong sum agg");
    }

    @Test
    public void findRows() {
        var table = CachedResultTable.create(csv, "|");
        ResultTable other = table.findRows("Total", "10.0");
        assertEquals(2, other.getRowCount(), "Wrong row count");

        other = table.findRows("Total", "14.0");
        assertEquals(1, other.getRowCount(), "Wrong row count");
        assertEquals("MSFT", other.getValue(0, "symbol"), "Wrong row key");
    }

    @Test
    public void getRow() {
        var table = CachedResultTable.create(csv, "|");
        assertEquals("[AAPL, 6.0, 10.0]", table.getRow(1, List.of("symbol", "AvgPrice", "Total")).toString(),
                "Wrong row vals");
    }

    @Test
    public void getNumber() {
        var table = CachedResultTable.create(csv, "|");
        assertEquals((Double) 5.0, table.getNumber(0, "AvgPrice"), "Expected Double value");
        assertEquals((Long) 1L, table.getNumber(1, "RowPosition"), "Expected Long value");
    }

    @Test
    public void toCsv() {
        var table = CachedResultTable.create(csv2, ",");
        assertEquals("""
        symbol|timestamp|ratio
        GIS|2023-09-08|5.0%
        AAPL|2023-09-08|6.0
        MSFT|2023-09-08|7.0%""", table.toCsv("|"), "Wrong csv output");
    }

    @Test
    public void toCsvJustified() {
        var table = CachedResultTable.create(csv2, ",");
        assertEquals("""
        symbol| timestamp|ratio
        GIS   |2023-09-08| 5.0%
        AAPL  |2023-09-08|  6.0
        MSFT  |2023-09-08| 7.0%""", table.toCsv("|", "LR"), "Wrong csv output");
    }

}
