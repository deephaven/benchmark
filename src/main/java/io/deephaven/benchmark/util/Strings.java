/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.util.*;

/**
 * Provide help with commonly used string manipulation
 */
public class Strings {
    /**
     * Covert all input objects to appropriate strings and make an array of strings
     * 
     * @param vals a list of values (including Collections) to be converted
     * @return an array of Strings
     */
    static public String[] toArray(Object... vals) {
        var arr = new ArrayList<>();
        for (Object val : vals) {
            if (!(val instanceof Collection)) {
                arr.add(val.toString());
                continue;
            }
            for (Object s : (Collection<?>) val) {
                arr.add(s.toString());
            }
        }
        return arr.toArray(new String[arr.size()]);
    }

    /**
     * Return a subset of <code>available</code> strings that start with the given <code>prefixes</code>. Throw an error
     * if any <code>prefixes</code> are not found in <code>available</code>.
     * 
     * @param available the list of strings to match against
     * @param prefixes prefixes that available strings may start with
     * @return an ordered set of available strings matched against prefixes
     */
    static public Set<String> startsWith(Collection<String> available, Collection<String> prefixes) {
        var matches = new LinkedHashSet<String>();
        for (String prefix : prefixes) {
            var found = available.stream().filter(a -> a.startsWith(prefix)).toList();
            if (found.isEmpty())
                throw new RuntimeException("Required prefix not found: " + prefix);
            matches.addAll(found);
        }
        return matches;
    }

}
