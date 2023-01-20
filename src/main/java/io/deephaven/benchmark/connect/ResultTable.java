package io.deephaven.benchmark.connect;

import java.util.List;

public interface ResultTable {
	public List<String> getColumnNames();
	public int getRowCount();
	public Object getValue(int rowIndex, String columnName);
	public Number getSum(String columnName);
	public ResultTable findRows(String columnName, Object value);
}
