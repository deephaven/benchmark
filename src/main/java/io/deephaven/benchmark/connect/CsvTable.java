/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.*;
import java.util.regex.Pattern;
import io.deephaven.benchmark.util.Numbers;

// Example TableTools.show data processed by this class
//
// RowPosition| RowKey| symbol| AvgPrice| Total| RecCount
// ----------+----------+----------+--------------------+----------+--------------------
// 0| 0|GIS | 5.0| 10.0| 2
// 1| 1|AAPL | 6.0| 12.0| 2
// 2| 2|MSFT | 7.0| 14.0| 2

public class CsvTable implements ResultTable {
    final List<String> columns;
    final List<List<Object>> rows;
    final String delim;

    public CsvTable(String csv, String delim) {
        List<String> lines = csv.lines().toList();
        delim = Pattern.quote(delim);
        this.columns = parseHeader(lines, delim);
        this.rows = parseRows(lines, delim);
        this.delim = delim;
    }

    private CsvTable(List<String> columns, List<List<Object>> rows, String delim) {
        this.columns = columns;
        this.rows = rows;
        this.delim = delim;
    }

    public List<String> getColumnNames() {
        return Collections.unmodifiableList(columns);
    }

    public int getRowCount() {
        return rows.size();
    }

    public Object getValue(int rowIndex, String columnName) {
        if (rowIndex >= rows.size())
            return null;
        return rows.get(rowIndex).get(getColumnIndex(columnName));
    }

    public Number getNumber(int rowIndex, String columnName) {
        Object val = getValue(rowIndex, columnName);
        return Numbers.parseNumber(val);
    }

    public ResultTable findRows(String columnName, Object value) {
        var matchedRows = new ArrayList<List<Object>>();

        int index = getColumnIndex(columnName);
        for (int i = 0, n = rows.size(); i < n; i++) {
            List<Object> row = rows.get(i);
            if (row.size() <= index)
                continue;
            if (row.get(index).equals(value))
                matchedRows.add(row);
        }
        return new CsvTable(columns, matchedRows, delim);
    }

    public Number getSum(String columnName) {
        int index = getColumnIndex(columnName);
        return rows.stream().mapToDouble(row -> Double.parseDouble(row.get(index).toString())).sum();
    }

    private int getColumnIndex(String columnName) {
        int index = columns.indexOf(columnName);
        if (index < 0)
            throw new RuntimeException("Undefined column name: " + columnName);
        return index;
    }

    private List<String> parseHeader(List<String> lines, String delim) {
        if (lines.isEmpty())
            return Collections.emptyList();
        return row(lines.get(0), delim).stream().map(s -> s.toString()).toList();
    }

    private List<List<Object>> parseRows(List<String> lines, String delim) {
        return lines.stream().skip(2).map(line -> row(line, delim)).toList();
    }

    private List<Object> row(String line, String delim) {
        return Arrays.stream(line.split(delim)).map(col -> (Object) col.trim()).toList();
    }

}
