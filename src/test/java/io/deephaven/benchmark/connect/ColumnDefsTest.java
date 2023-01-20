package io.deephaven.benchmark.connect;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

import io.deephaven.benchmark.connect.ColumnDefs;

public class ColumnDefsTest {

	@Test
	public void add() {
		ColumnDefs columnDefs = new ColumnDefs()
			.add("symbol", "string", "ABC[1-11]")
			.add("price", "float", "[100-105]")
			.add("priceAgain", "int", "[100-105]");
		
		assertEquals(3, columnDefs.columns.size(), "Wrong def count");
		
		assertEquals("string", columnDefs.columns.get(0).type(), "Wrong field count");
		assertEquals("symbol", columnDefs.columns.get(0).name(), "Wrong field name");
		assertEquals("StringMaker", columnDefs.columns.get(0).maker().getClass().getSimpleName(), "Wrong field maker");
		assertEquals("ABC6", columnDefs.nextValue(0, 1), "Wrong field next");
		
		assertEquals("float", columnDefs.columns.get(1).type(), "Wrong field count");
		assertEquals("price", columnDefs.columns.get(1).name(), "Wrong field name");
		assertEquals("FloatMaker", columnDefs.columns.get(1).maker().getClass().getSimpleName(), "Wrong field maker");
		assertEquals("[100.0, 101.0, 102.0, 103.0, 104.0, 105.0]", columnDefs.columns.get(1).maker().values.toString(), "Wrong field maker");
		assertEquals(100.0f, columnDefs.nextValue(1, 1), "Wrong field next");
		
		assertEquals("[100, 101, 102, 103, 104, 105]", columnDefs.columns.get(2).maker().values.toString(), "Wrong field maker");
		assertEquals(103, columnDefs.nextValue(2, 1), "Wrong field next");
		assertEquals(105, columnDefs.nextValue(2, 0), "Wrong field next");
	}
	
	@Test
	public void getQuotedColumns() {
		ColumnDefs columnDefs = new ColumnDefs()
			.add("symbol", "string", "ABC[1-11]")
			.add("price", "float", "[100-105]")
			.add("priceAgain", "int", "[100-105]");
		
		assertEquals("\"symbol\", \"price\", \"priceAgain\"", columnDefs.getQuotedColumns(), "Wrong field next");
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
			.add("priceAgain", "int", "[100-105]");
		
		assertEquals( 
			"""
			name,type,values
			symbol,string,ABC[1-10]
			price,float,[100-105]
			priceAgain,int,[100-105]
			""", 
			columnDefs.describe(), "Wrong toString");
		
		columnDefs.setFixed();
		
		assertEquals(
			"""
			name,type,values
			symbol,string,ABC[1-10]
			price,float,[100-105]
			priceAgain,int,[100-105]
			""", 
			columnDefs.describe(), "Wrong toString");
	}
	
}
