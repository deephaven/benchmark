/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provide unique Ids for file naming
 */
public class Ids {
    static final Random random = new Random();
    static final AtomicInteger delta = new AtomicInteger(new Random().nextInt(100000, 999999));

    /**
     * Replace any characters in the given name that may not be safe to use as a file name
     * 
     * @param name prospective file name
     * @return a name with file name safe characters
     */
    static public String getFileSafeName(String name) {
        return name.replaceAll("[^A-Za-z0-9_.-]", "_");
    }

    /**
     * Create a time-based id suitable for file and directory naming
     * 
     * @return a time-based id
     */
    static public String runId() {
        return "run-" + nowBase62();
    }

    /**
     * Determine if the given object string looks like an id created with <code>runId()</code>
     * 
     * @param id the possible run id
     * @return true if it looks like a runId, otherwise false
     */
    static public boolean isRunId(Object id) {
        return id.toString().matches("run-[0-9A-Za-z]+");
    }

    /**
     * Make a unique time-based identifier (not a UUID). Successive calls guarantee a unique name returned within a
     * single JVM.
     * <p/>
     * ex. PREFIX.UIuyguJ.2cOP
     * 
     * @return the unique name
     */
    static public String uniqueName(String prefix) {
        String time = nowBase62();
        String d = Numbers.toBase62(Math.abs(delta.incrementAndGet()));
        return String.join(".", prefix, time, d);
    }

    /**
     * Return a 64 bit hashcode for the given char sequence
     * 
     * @param s char sequence to hash
     * @return a long hash
     */
    static public long hash64(CharSequence s) {
        long h = 0;
        for (int i = 0, n = s.length(); i < n; i++) {
            h = 31 * h + s.charAt(i);
        }
        return h;
    }

    static String nowBase62() {
        var now = Instant.now();
        return Numbers.toBase62(String.format("%d%03d", now.getEpochSecond(), now.getNano() / 1000000));
    }

}
