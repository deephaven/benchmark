/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.List;

/**
 * Table used to fetch during or after executing a query through a connector (e.g. <code>BarrageConnector</code>) or
 * through the Bench API
 * <p/>
 * ex. api.query(query).fetchAfter("myTableName", table -> { // do something }).execute();
 */
public interface ResultTable {
    /**
     * Get a list of the column names defined for the table
     * 
     * @return a list of column names
     */
    public List<String> getColumnNames();

    /**
     * Get the row count of the table
     * 
     * @return the table row count
     */
    public int getRowCount();

    /**
     * Get a value from a row and column in the table
     * 
     * @param rowIndex a row index starting from 0
     * @param columnName the case-sensitive column name
     * @return the corresponding value or null
     */
    public Object getValue(int rowIndex, String columnName);

    /**
     * Return the column value in a row as a number. Return <code>Double</code> or <code>Long</code> depending on the
     * existence of decimal
     * 
     * @param rowIndex a row index starting from 0
     * @param columnName the case-sensitive column name
     * @return the corresponding value or null
     */
    public Number getNumber(int rowIndex, String columnName);

    /**
     * Get a aggregate sum for all rows for the given column
     * 
     * @param columnName the column to sum
     * @return the result of the column aggregation
     */
    public Number getSum(String columnName);

    /**
     * Get a subtable of this table containing all the rows that match the given column name and value. These returned
     * table does not contain copies of the original rows.
     * 
     * @param columnName the column to match
     * @param value the value to match
     * @return all rows that match
     */
    public ResultTable findRows(String columnName, Object value);

    /**
     * Return values for the given columns in the order they are listed
     * 
     * @param rowIndex the row position starting at 0
     * @param columNames the column names to use for value retrieval
     * @return the column values for the row
     */
    public List<Object> getRow(int rowIndex, List<String> columnNames);

    /**
     * Return a string containing the Csv representation of this table.
     * 
     * @param delim the delimiter to use between column
     * @return a csv string
     */
    public String toCsv(String delim);

    /**
     * Return a string containing the Csv representation of this table. Allow setting column justification to right or
     * left depending on the 'R' or 'L' specified in the alignment description.
     * <p/>
     * ex. "|", "LRRRRL" where there are 6 columns in the data
     * 
     * @param delim the delimiter to use between column
     * @param alignDescr a string of R's (right justified) and L's (left justified for each column
     * @return a csv string
     */
    public String toCsv(String delim, String alignDescr);

}
