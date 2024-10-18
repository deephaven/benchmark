/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import io.deephaven.benchmark.util.Dates;
import io.deephaven.benchmark.util.Numbers;
import io.deephaven.engine.rowset.RowSet;
import io.deephaven.engine.table.Table;

/**
 * Create an in-memory table from either CSV or a Deephaven table. Provides some basic accessors for getting column
 * values. No data typing is done on import of the data. Use typed methods like {@code getNumber()} to convert from
 * whatever row value came from the import.
 * <p>
 * Note: This class is not a general purpose class for reading CSV or Deephaven Table data. It fits specific cases used
 * by the Benchmark framework.
 */
public class CachedResultTable implements ResultTable {

    /**
     * Create an in-memory table instance from basic CSV. Does not handle quotes and is mainly used for testing. Skips
     * any lines that do not have the same items as the header and trims all row items. No attempt is made to determine
     * data types.
     * 
     * @param csv basic csv with a header and columns
     * @param delim the delimeter to use between columns
     * @return a cached result table instance
     */
    static public ResultTable create(String csv, String delim) {
        var importer = new CsvImporter(csv, delim);
        int minRowColumnCount = importer.columns().size();
        return new CachedResultTable(importer.columns(), importer.rows(minRowColumnCount));
    }

    /**
     * Create an in-memory table instance from a Deephaven Engine Table. Whatever datatype is read from the Table is
     * stored in this cache.
     * 
     * @param table a Deephaven table (likely procured from a subscription)
     * @return a cached result table
     */
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
        return getNonFormatColumns(columns);
    }

    public int getRowCount() {
        return rows.size();
    }

    public Object getValue(int rowIndex, String columnName) {
        if (rowIndex >= rows.size())
            return null;
        var value = rows.get(rowIndex).get(getColumnIndex(columnName));
        var regex = columnName + "__.*_FORMAT";
        var formatNames = columns.stream().filter(c -> c.matches(regex)).toList();
        if (!formatNames.isEmpty()) {
            var formatVal = rows.get(rowIndex).get(getColumnIndex(formatNames.get(0)));
            var formatName = formatNames.get(0).replace(columnName + "__", "");
            value = formatValue(value, formatName, formatVal.toString());
        }
        return value;
    }

    public List<Object> getRow(int rowIndex, List<String> columnNames) {
        return getNonFormatColumns(columnNames).stream().map(c -> getValue(rowIndex, c)).toList();
    }

    public Number getNumber(int rowIndex, String columnName) {
        Object val = getValue(rowIndex, columnName);
        return Numbers.parseNumber(val);
    }

    public ResultTable findRows(String columnName, Object value) {
        var matchedRows = new ArrayList<List<Object>>();

        int index = getColumnIndex(columnName);
        for (List<Object> row : rows) {
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

    public String toCsv(String delim) {
        return toCsv(delim, null);
    }

    public String toCsv(String delim, String alignment) {
        var alignMeta = getColumnAlignments(alignment);
        var csv = new StringBuilder();
        var columnNames = getColumnNames();
        csv.append(String.join(delim, applyAlignments(columnNames, alignMeta))).append('\n');
        for (int i = 0, n = getRowCount(); i < n; i++) {
            if (i > 0)
                csv.append('\n');
            List<Object> row = getRow(i, columnNames);
            List<String> newRow = applyAlignments(row, alignMeta);
            csv.append(String.join(delim, newRow));
        }
        return csv.toString();
    }

    private String formatValue(Object value, String formatName, String formatValue) {
        if (formatName.equals("TABLE_DATE_FORMAT"))
            return Dates.formatDate(value, formatValue);
        if (formatName.equals("TABLE_NUMBER_FORMAT"))
            return Numbers.formatNumber(value, formatValue);
        throw new RuntimeException("Unsupported table format: " + formatName);
    }

    private int getColumnIndex(String columnName) {
        int index = columns.indexOf(columnName);
        if (index < 0)
            throw new RuntimeException("Undefined column name: " + columnName);
        return index;
    }

    private List<String> getNonFormatColumns(List<String> columns) {
        var regex = ".*__TABLE_.*_FORMAT";
        return columns.stream().filter(n -> !n.matches(regex)).toList();
    }

    private List<Alignment> getColumnAlignments(String alignDescr) {
        if (alignDescr == null || alignDescr.isBlank())
            return null;
        var columns = getColumnNames();
        var alignments = new ArrayList<Alignment>(columns.size());
        for (int c = 0, cn = columns.size(); c < cn; c++) {
            var alignChar = (c >= alignDescr.length()) ? 'R' : alignDescr.charAt(c);
            int maxWidth = columns.get(c).length();
            for (int i = 0, n = getRowCount(); i < n; i++) {
                maxWidth = Math.max(getValue(i, columns.get(c)).toString().length(), maxWidth);
            }
            alignments.add(new Alignment(alignChar, maxWidth));
        }
        return alignments;
    }

    private List<String> applyAlignments(List<?> row, List<Alignment> alignMeta) {
        if (alignMeta == null || alignMeta.isEmpty())
            return row.stream().map(c -> c.toString()).toList();
        var newRow = new ArrayList<String>(row.size());
        for (int i = 0, n = alignMeta.size(); i < n; i++) {
            var value = row.get(i).toString();
            var align = alignMeta.get(i);
            var space = " ".repeat(align.width - value.length());
            value = (alignMeta.get(i).direction == 'L') ? (value + space) : (space + value);
            newRow.add(value);
        }
        return newRow;
    }

    record Alignment(char direction, int width) {
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

        List<List<Object>> rows(int minColumns) {
            return lines.stream().skip(1).map(line -> row(line, delim)).filter(r -> r.size() >= minColumns).toList();
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
