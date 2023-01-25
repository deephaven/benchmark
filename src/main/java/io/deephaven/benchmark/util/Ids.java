/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.util;

import java.math.BigInteger;
import java.util.Base64;
import java.util.Random;

/**
 * Provide unique Ids for file naming
 */
public class Ids {
    static final long years50 = 1577847600000L;
    static final Random random = new Random();
    static private long count = 0;
    static private long lastTime = now50();

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
        return "run-" + Long.toHexString(now50());
    }

    /**
     * Determine if the given object string looks like an id created with <code>runId()</code>
     * 
     * @param id the possible run id
     * @return true if it looks like a runId, otherwise false
     */
    static public boolean isRunId(Object id) {
        return id.toString().matches("run-[a-z0-9]{10,16}");
    }

    /**
     * Return a unique identifier (not a UUID) ex. Fd7YDsw.1.bjSAVA
     * 
     * @return the unique name
     */
    static public String uniqueName() {
        long now = now50();
        if (now > lastTime) {
            lastTime = now;
            count = 0;
        }

        String time = toBase64(now);
        String cnt = Long.toHexString(++count);
        String rand = toBase64(random.nextInt());
        return time + '.' + cnt + '.' + rand;
    }

    static String toBase64(long num) {
        String s = Base64.getUrlEncoder().encodeToString(BigInteger.valueOf(num).toByteArray());
        return s.replace("=", "");
    }

    static long now50() {
        return System.currentTimeMillis() - years50;
    }

}
