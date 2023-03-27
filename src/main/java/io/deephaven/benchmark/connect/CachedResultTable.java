/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import io.deephaven.benchmark.util.Numbers;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.table.Table;

public class CachedResultTable implements ResultTable {

    static public ResultTable create(String csv, String delim) {
        var importer = new CsvImporter(csv, delim);
        return new CachedResultTable(importer.columns(), importer.rows());
    }

    static public ResultTable create(Table table) {
        var importer = new EngineTableImporter(table);
        return new CachedResultTable(importer.columns(), importer.rows());
    }

    final List<String> columns;
    final List<List<Object>> rows;

    CachedResultTable(List<String> columns, List<List<Object>> rows) {
        this.columns = columns;
        this.rows = rows;
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
        return new CachedResultTable(columns, matchedRows);
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

    static class CsvImporter {
        final List<String> lines;
        final String delim;

        CsvImporter(String csv, String delim) {
            this.lines = csv.lines().toList();
            this.delim = Pattern.quote(delim);
        }

        List<String> columns() {
            if (lines.isEmpty())
                return Collections.emptyList();
            return row(lines.get(0), delim).stream().map(s -> s.toString()).toList();
        }

        List<List<Object>> rows() {
            if (lines.size() < 3)
                return Collections.emptyList();
            return lines.stream().skip(2).map(line -> row(line, delim)).toList();
        }

        private List<Object> row(String line, String delim) {
            return Arrays.stream(line.split(delim)).map(col -> (Object) col.trim()).toList();
        }
    }

    static class EngineTableImporter {
        final Table source;

        EngineTableImporter(Table source) {
            this.source = source;
        }

        List<String> columns() {
            return source.getDefinition().getColumnNames();
        }

        List<List<Object>> rows() {
            List<List<Object>> rows = new ArrayList<>();
            List<String> columns = columns();
            RowSet.Iterator iter = source.getRowSet().iterator();
            while (iter.hasNext()) {
                long key = iter.nextLong();
                List<Object> row = new ArrayList<>(columns.size());
                for (String column : columns) {
                    row.add(source.getColumnSource(column).get(key));
                }
                rows.add(row);
            }
            return rows;
        }
    }

}
