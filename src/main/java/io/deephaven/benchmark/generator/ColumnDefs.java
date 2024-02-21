/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.generator;

import java.util.*;
import io.deephaven.benchmark.util.Numbers;

/**
 * Contains column definitions used to generate data and schemas. Columns are described by name, type, and data range
 * (ex. "[1-100]", "str[1-100]ing"). Values are retrieved during data generation either randomly or incrementally
 * through the range. The same seed is used for random each time this class is instantiated.
 * <p/>
 * Note: All possible data values are loaded up front to prevent object-creation during production. This can take a
 * considerable amount of memory for larger scales, especially for generated strings.
 * <p/>
 */
public class ColumnDefs {
    final int valueCacheSize;
    final List<ColumnDef> columns = new ArrayList<>();
    private String defaultDistribution = "random";

    public ColumnDefs() {
        this(1024);
    }

    ColumnDefs(int valueCacheSize) {
        this.valueCacheSize = valueCacheSize;
    }

    /**
     * Get the number of column definitions.
     * 
     * @return the number of column definitions
     */
    public int getCount() {
        return columns.size();
    }

    /**
     * Set the default column distribution for columns that do not have a distribution defined.
     * 
     * @param distribution the distribution name (e.g. {@code random | incremental})
     */
    public void setDefaultDistribution(String distribution) {
        defaultDistribution = distribution;
    }

    /**
     * Get the maximum possible number of values represented by the ranges in all column definitions. For example, given
     * two column ranges [1-10] and [10-30], the count would be 20. Put another way, it's the largest range for all
     * column definitions in this set.
     * 
     * @return the maximum number of values defined in this set
     */
    public long getMaxValueCount() {
        return columns.stream().mapToLong(c -> c.maker.getDefSize()).max().getAsLong();
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
     * Add a new column definition.
     * 
     * @param name the column name
     * @param type the column type
     * @param valueDef the range data (ex. "[1-10]", "str[1-100]ing")
     * @param distribution override default distribution function (e.g. random, incremental) with another one, or null
     * @return this
     */
    public ColumnDefs add(String name, String type, String valueDef, String distribution) {
        var maker = getMaker(type, valueDef);
        maker.setDistribution(distribution, name + ':' + type + ':' + valueDef);
        columns.add(new ColumnDef(name, type, valueDef, maker));
        return this;
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
        return add(name, type, valueDef, null);
    }

    /**
     * Get the next value for the column in the given index according to the columns defined distribution.
     * 
     * @param columnIndex the index of the column
     * @param seed a value to use to get the next value (e.g. row id)
     * @param the maximum value that could be used as a seed (e.g. row count)
     * @return the next value according to the column definition
     */
    public Object nextValue(int columnIndex, long seed, long max) {
        return columns.get(columnIndex).maker().next(seed, max);
    }

    /**
     * Get the column definitions as a string. It intentionally avoids OS-specific line endings.
     * <p/>
     * Note: This method is used to write table definitions for comparison to the file system. Do not change without
     * understanding the impact.
     * 
     * @return a string describing this column definition
     */
    public String describe() {
        var str = "name,type,values,distribution\n";
        for (ColumnDef c : columns) {
            var distribution = c.maker.getDistribution().toLowerCase();
            str += String.join(",", c.name(), c.type(), c.valueDef(), distribution) + "\n";
        }
        return str;
    }

    private Maker getMaker(String type, String valueDef) {
        ValueDef def = parseValueDef(valueDef);
        return switch (type.toLowerCase()) {
            case "string" -> new StringMaker(def);
            case "long" -> new LongMaker(def);
            case "int" -> new IntMaker(def);
            case "double" -> new DoubleMaker(def);
            case "float" -> new FloatMaker(def);
            case "timestamp-millis" -> new TimestampMaker(def);
            default -> throw new RuntimeException("Invalid field type: " + type);
        };
    }

    // "[1-10]"
    private ValueDef parseValueDef(String valueDef) {
        String bracketMatch = ".*(\\[[0-9]+[-][0-9]+\\]).*";
        if (!valueDef.matches(bracketMatch))
            return new ValueDef(0, 1, null, valueDef, true);
        String brackets = valueDef.replaceAll(bracketMatch, "$1");
        String[] range = brackets.replaceAll(".*\\[([0-9]+)[-]([0-9]+)\\].*", "$1,$2").split(",");
        if (range.length != 2)
            return new ValueDef(0, 1, null, valueDef, true);
        long rangeStart = Long.parseLong(range[0]);
        long rangeEnd = Long.parseLong(range[1]) + 1; // End is inclusive

        return new ValueDef(rangeStart, rangeEnd - rangeStart, brackets, valueDef, false);
    }

    private Object adjustValueSymmetry(Object value, String distribution, ValueDef def) {
        if (value instanceof String || def.isLiteral)
            return value;

        return switch (distribution) {
            case "ascending" -> value; // All positives
            case "descending" -> Numbers.negate(value); // All negatives
            // case "random-even-neg" -> Numbers.isEven(value) ? Numbers.negate(value) : value; // Negate evens
            case "random" -> !Numbers.isEven(value) ? Numbers.negate(value) : value; // Negate odds
            // case "random-shift" -> (int) ((Number) value).intValue() - (int) (def.size() / 2);
            // case "random" -> value;
            case "runlength" -> Numbers.isEven(value) ? Numbers.negate(value) : value; // Negate evens
            default -> throw new RuntimeException("Undefined distribution function name: " + distribution);
        };
    }

    record ColumnDef(String name, String type, String valueDef, Maker maker) {
    }

    class StringMaker extends Maker {
        StringMaker(ValueDef def) {
            super(def);
        }

        @Override
        String value(long index) {
            return def.getString(index);
        }
    }

    class LongMaker extends Maker {
        LongMaker(ValueDef def) {
            super(def);
        }

        @Override
        Long value(long index) {
            return def.getLong(index);
        }
    }

    class IntMaker extends Maker {
        IntMaker(ValueDef def) {
            super(def);
        }

        @Override
        Integer value(long index) {
            return (int) def.getLong(index);
        }
    }

    class DoubleMaker extends Maker {
        DoubleMaker(ValueDef def) {
            super(def);
        }

        @Override
        Double value(long index) {
            return (double) def.getLong(index);
        }
    }

    class FloatMaker extends Maker {
        FloatMaker(ValueDef def) {
            super(def);
        }

        @Override
        Float value(long index) {
            return (float) def.getLong(index);
        }
    }

    class TimestampMaker extends Maker {
        TimestampMaker(ValueDef def) {
            super(def);
        }

        @Override
        Long value(long index) {
            return def.getLong(index);
        }
    }

    abstract class Maker {
        final Object[] cache;
        final ValueDef def;
        final int maxCacheSize;
        private String distributionName = null;
        private String distributionId = null;
        private DFunction distribution = null;

        Maker(ValueDef def) {
            this.def = def;
            this.maxCacheSize = (int) Math.min(def.size(), valueCacheSize);
            this.cache = new Object[maxCacheSize];
        }

        abstract Object value(long index);

        final Object next(long seed, long max) {
            ensureDistributionFunc();
            long index = (long) distribution.apply(0, max, seed, 0, def.size());
            return (index < cache.length && cache[(int) index] != null) ? cache[(int) index] : getValue(index);
        }

        private Object getValue(long index) {
            Object v = adjustValueSymmetry(value(index), distributionName, def);

            if (index < cache.length)
                cache[(int) index] = v;
            return v;
        }

        private long getDefSize() {
            return def.size();
        }

        private String getDistribution() {
            ensureDistributionFunc();
            return distributionName;
        }

        private void setDistribution(String distribution, String id) {
            this.distributionName = (distribution == null) ? null : distribution.toLowerCase();
            this.distributionId = id;
        }

        private void ensureDistributionFunc() {
            if (distribution == null) {
                distributionName = (distributionName == null) ? defaultDistribution : distributionName;
                var dname = distributionName.equals("descending") ? "ascending" : distributionName;
                distribution = DFunction.get(dname, distributionId);
            }
        }
    }

    record ValueDef(long rangeStart, long size, String brackets, String def, boolean isLiteral) {
        String getString(long index) {
            return (isLiteral) ? def : def.replace(brackets, Long.toString(index + rangeStart));
        }

        long getLong(long index) {
            return (isLiteral) ? Long.valueOf(def) : (index + rangeStart);
        }
    }

}
