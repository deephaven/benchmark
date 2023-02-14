/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.connect.ResultTable;

/**
 * Collects various properties about the running client and server used during a benchmark run and stores them in the
 * benchmark results directory.
 */
class Platform {
    static final String platformFileName = "benchmark-platform.csv";
    static final String[] header = {"application", "name", "value"};
    final Path platformFile;
    private boolean hasBeenCommitted = false;

    /**
     * Initialize platform detail collection with the default result file name.
     * 
     * @param parent the parent directory of the platform file
     */
    Platform(Path parent) {
        this(parent, platformFileName);
    }

    /**
     * Initialize platform detail collection using the given result file name
     * 
     * @param parent the parent directory of the platform file
     * @param platformFileName the name the file to store platform properties
     */
    Platform(Path parent, String platformFileName) {
        this.platformFile = parent.resolve(platformFileName);
    }

    /**
     * Ensure that collected plaform properties have been saved
     */
    void ensureCommit() {
        if (hasBeenCommitted)
            return;
        hasBeenCommitted = true;
        try (BufferedWriter out = Files.newBufferedWriter(platformFile)) {
            out.write(String.join(",", header));
            out.newLine();
            writeTestProps(out);
            writeEngineProps(out);
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write platform file: " + platformFile, ex);
        }
    }

    /**
     * Get a table for a query that has been filled with server-side properties
     * 
     * @param query the query used to get/make the property table
     * @return a cached result table containing properties
     */
    protected ResultTable fetchResult(String query) {
        Bench api = new Bench(Bench.class);
        api.setName("# Write Platform Details"); // # means skip adding to results file

        var tbl = new AtomicReference<ResultTable>();
        api.query(query).fetchAfter("pil", table -> {
            tbl.set(table);
        }).execute();
        api.close();

        return tbl.get();
    }

    private void writeTestProps(BufferedWriter out) throws Exception {
        String type = "test-runner";
        println(out, type, "java.version", System.getProperty("java.version"));
        println(out, type, "java.vm.name", System.getProperty("java.vm.name"));
        println(out, type, "java.class.version", System.getProperty("java.class.version"));
        println(out, type, "os.name", System.getProperty("os.name"));
        println(out, type, "os.version", System.getProperty("os.version"));
        println(out, type, "available.processors", "" + Runtime.getRuntime().availableProcessors());
        println(out, type, "java.max.memory", gigs(Runtime.getRuntime().maxMemory()));
    }

    private void writeEngineProps(BufferedWriter out) throws Exception {
        var query = """
        import deephaven.perfmon as pm
        from deephaven import empty_table

        pil = pm.process_info_log().snapshot()
        """;

        ResultTable t = fetchResult(query);

        String type = "deephaven-engine";
        println(out, type, "java.version", getTableValue(t, "runtime-mx.sys-props", "java.version"));
        println(out, type, "java.vm.name", getTableValue(t, "runtime-mx.sys-props", "java.vm.name"));
        println(out, type, "java.class.version", getTableValue(t, "runtime-mx.sys-props", "java.class.version"));
        println(out, type, "os.name", getTableValue(t, "runtime-mx.sys-props", "os.name"));
        println(out, type, "os.version", getTableValue(t, "runtime-mx.sys-props", "os.version"));
        println(out, type, "available.processors", getTableValue(t, "system-info.cpu", "logical"));
        println(out, type, "java.max.memory", gigs(getTableValue(t, "memory-mx.heap", "max")));
    }

    private String getTableValue(ResultTable table, String type, String key) {
        table = table.findRows("Key", key);
        table = table.findRows("Type", type);
        return table.getValue(0, "Value").toString();
    }

    private void println(BufferedWriter out, String type, String name, String value) throws Exception {
        out.write(String.join(",", type, name, value));
        out.newLine();
    }

    private String gigs(Object num) {
        var g = (float) (Long.parseLong(num.toString().trim()) / 1024.0 / 1024.0 / 1024.0);
        return String.format("%.2f", g) + "G";
    }

}
