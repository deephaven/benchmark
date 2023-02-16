/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.*;
import java.util.stream.LongStream;

/**
 * Contains column definitions used to generate data and schemas. Columns are described by name, type, and data range
 * (ex. "[1-100]", "str[1-100]ing"). Values are retrieved during data generation either randomly or incrementally
 * through the range. The same seed is used for random each time this class is instantiated.
 * <p/>
 * Note: All possible data values are loaded up front to prevent object-creation during production. This can take a
 * considerable amount of memory for larger scales, especially for generated strings.
 */
public class ColumnDefs {
    final Random random = new Random(20221130);
    final List<ColumnDef> columns = new ArrayList<>();
    private boolean isFixed = false;

    /**
     * Get the number of column definitions.
     * 
     * @return the number of column definitions
     */
    public int getCount() {
        return columns.size();
    }

    /**
     * Get whether or not column values are selected incrementally through the range or random
     * 
     * @return true if incremental/fixed, otherwise false.
     */
    public boolean isFixed() {
        return isFixed;
    }

    /**
     * Get the maximum possible number of values represented by the ranges in all column definitions. For example, given
     * two column ranges [1-10] and [10-30], the count would be 20. Put another way, it's the largest range for all
     * column definitions in this set.
     * 
     * @return the maximum number of values defined in this set
     */
    public int getMaxValueCount() {
        return columns.stream().mapToInt(c -> c.maker.values.size()).max().getAsInt();
    }

    /**
     * Get a comma-separated list of quoted column names in this set
     * 
     * @return quoted column names.
     */
    public String getQuotedColumns() {
        return String.join(",", columns.stream().map(c -> "\"" + c.name + "\"").toList());
    }

    /**
     * Get a map containing the name and type for each column in this definition set
     * 
     * @return column names and types as a map.
     */
    public Map<String, String> toTypeMap() {
        var typeMap = new LinkedHashMap<String, String>();
        columns.stream().forEach(f -> typeMap.put(f.name(), f.type()));
        return typeMap;
    }

    /**
     * Set all column definitions to generate data incrementally.
     */
    public void setFixed() {
        isFixed = true;
    }

    /**
     * Set all column definitions to generate data randomly.
     */
    public void setRandom() {
        isFixed = false;
    }

    /**
     * Add a new column definition.
     * 
     * @param name the column name
     * @param type the column type
     * @param valueDef the range data (ex. "[1-10]", "str[1-100]ing")
     * @return this
     */
    public ColumnDefs add(String name, String type, String valueDef) {
        columns.add(new ColumnDef(name, type, valueDef, getMaker(type, valueDef)));
        return this;
    }

    /**
     * Get the next value for the column in the given index. This supports incremental and random retrieval of the
     * column next column value according to <code>isFixed</code>
     * 
     * @param columnIndex the index of the column
     * @param seed a value to use to get the next value (typically row id)
     * @return the next value according to the column definition
     */
    public Object nextValue(int columnIndex, long seed) {
        return columns.get(columnIndex).maker().next(seed);
    }

    /**
     * Get the column definitions as a string. It intentionall avoids OS-specific line endings.
     * <p/>
     * Note: This method is used to write table definitions for comparison to the file system. Do not change without
     * understanding the impact.
     * 
     * @return a
     */
    public String describe() {
        String str = "name,type,values\n";
        for (ColumnDef c : columns) {
            str += String.join(",", c.name(), c.type(), c.valueDef()) + "\n";
        }
        return str;
    }

    private Maker getMaker(String type, String valueDef) {
        List<String> values = getParsedValues(valueDef);
        switch (type.toLowerCase()) {
            case "string":
                return new StringMaker(values);
            case "long":
                return new LongMaker(values);
            case "int":
                return new IntMaker(values);
            case "double":
                return new DoubleMaker(values);
            case "float":
                return new FloatMaker(values);
            case "timestamp-millis":
                return new TimestampMaker(values);
            default:
                throw new RuntimeException("Invalid field type: " + type);
        }
    }

    // "[1-10]"
    private List<String> getParsedValues(String valueDef) {
        String bracketMatch = ".*(\\[[0-9]+[-][0-9]+\\]).*";
        if (!valueDef.matches(bracketMatch))
            return List.of(valueDef);
        String brackets = valueDef.replaceAll(bracketMatch, "$1");
        String[] range = brackets.replaceAll(".*\\[([0-9]+)[-]([0-9]+)\\].*", "$1,$2").split(",");
        if (range.length != 2)
            return List.of(valueDef);
        long rangeStart = Long.parseLong(range[0]);
        long rangeEnd = Long.parseLong(range[1]) + 1; // End is inclusive

        return LongStream.range(rangeStart, rangeEnd).mapToObj(v -> valueDef.replace(brackets, Long.toString(v)))
                .toList();
    }

    record ColumnDef(String name, String type, String valueDef, Maker maker) {
    }

    class StringMaker extends Maker {
        StringMaker(List<String> values) {
            super(values.stream().map(v -> (Object) v).toList());
        }
    }

    class LongMaker extends Maker {
        LongMaker(List<String> values) {
            super(values.stream().map(v -> (Object) Long.valueOf(v)).toList());
        }
    }

    class IntMaker extends Maker {
        IntMaker(List<String> values) {
            super(values.stream().map(v -> (Object) Integer.valueOf(v)).toList());
        }
    }

    class DoubleMaker extends Maker {
        DoubleMaker(List<String> values) {
            super(values.stream().map(v -> (Object) Double.valueOf(v)).toList());
        }
    }

    class FloatMaker extends Maker {
        FloatMaker(List<String> values) {
            super(values.stream().map(v -> (Object) Float.valueOf(v)).toList());
        }
    }

    class TimestampMaker extends Maker {
        TimestampMaker(List<String> values) {
            super(values.stream().map(v -> (Object) Long.valueOf(v)).toList());
        }
    }

    abstract class Maker {
        final List<Object> values;

        Maker(List<Object> values) {
            this.values = values;
        }

        public Object next(long seed) {
            if (isFixed)
                return values.get((int) (seed % values.size()));
            return values.get(random.nextInt(0, values.size()));
        }
    }

}
