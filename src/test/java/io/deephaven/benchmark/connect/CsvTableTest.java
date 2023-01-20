package io.deephaven.benchmark.connect;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;

import io.deephaven.benchmark.connect.CsvTable;
import io.deephaven.benchmark.connect.ResultTable;

public class CsvTableTest {
	String csv = 
		"""
		RowPosition|     RowKey|    symbol|            AvgPrice|     Total|            RecCount
		----------+----------+----------+--------------------+----------+--------------------
	         0|         0|GIS       |                 5.0|      10.0|                   2
	         1|         1|AAPL      |                 6.0|      10.0|                   2
	         2|         2|MSFT      |                 7.0|      14.0|                   2
		""";
	
	@Test
	public void fromBarrageCsv() {
		CsvTable table = new CsvTable(csv, "|");
		
		assertEquals("[RowPosition, RowKey, symbol, AvgPrice, Total, RecCount]", table.getColumnNames().toString(), "Wrong columns");
		assertEquals(3, table.getRowCount(), "Wrong row count");
		assertEquals("0", table.getValue(0, "RowPosition"), "Wrong row value");
		assertEquals("MSFT", table.getValue(2, "symbol"), "Wrong row value");
		assertEquals(18.0, table.getSum("AvgPrice"), "Wrong sum agg");
	}
	
	@Test
	public void findRows() {
		CsvTable table = new CsvTable(csv, "|");
		ResultTable other = table.findRows("Total", "10.0");
		assertEquals(2, other.getRowCount(), "Wrong row count");
		
		other = table.findRows("Total", "14.0");
		assertEquals(1, other.getRowCount(), "Wrong row count");
		assertEquals("MSFT", other.getValue(0, "symbol"), "Wrong row key");
	}

}
