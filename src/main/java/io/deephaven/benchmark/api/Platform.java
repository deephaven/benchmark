/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.engine.exceptions.ArgumentException;

/**
 * Collects various properties about the running client and server used during a benchmark run and stores them in the
 * benchmark results directory.
 */
class Platform {
    static final String platformFileName = "benchmark-platform.csv";
    static final String[] header = {"origin", "name", "value"};
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
    void commit() {
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
        api.query(query).fetchAfter("benchApiProps", table -> {
            tbl.set(table);
        }).execute();
        api.close();

        return tbl.get();
    }

    private void writeTestProps(BufferedWriter benchApiProps) throws Exception {
        var dhInst = new ArgumentException();
        var benchApiOrigin = "test-runner";
        var deephavenVersion = getDeephavenVersion(dhInst);

        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.version", System.getProperty("java.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.vm.name", System.getProperty("java.vm.name"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.class.version",
                System.getProperty("java.class.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "os.name", System.getProperty("os.name"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "os.version", System.getProperty("os.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "available.processors",
                Runtime.getRuntime().availableProcessors());
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.max.memory", Runtime.getRuntime().maxMemory());
        benchApiAddProperty(benchApiProps, benchApiOrigin, "deephaven.version", deephavenVersion);
    }

    private void writeEngineProps(BufferedWriter out) throws Exception {
        var query = """
        import jpy
        from deephaven import new_table, input_table
        from deephaven import dtypes as dht
        from deephaven.column import string_col
        
        def benchApiAddProperty(prop_table, origin, name, value):
            t = new_table([string_col('origin', [origin]), string_col('name', [name]), string_col('value', [str(value)])])
            prop_table.add(t)
        
        System = jpy.get_type('java.lang.System')
        Runtime = jpy.get_type('java.lang.Runtime')
        
        dhInst = jpy.get_type('io.deephaven.engine.exceptions.ArgumentException')()
        benchApiOrigin = 'deephaven-engine'
        deephavenVersion = dhInst.getClass().getPackage().getImplementationVersion()
        
        benchApiProps = input_table({'origin':dht.string, 'name':dht.string, 'value':dht.string})
        
        python_version = '.'.join([str(sys.version_info.major), str(sys.version_info.minor), str(sys.version_info.micro)])
        benchApiAddProperty(benchApiProps, benchApiOrigin, "python.version", python_version);
        
        # Java Properties
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.version", System.getProperty("java.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.vm.name", System.getProperty("java.vm.name"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.class.version",
                System.getProperty("java.class.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "os.name", System.getProperty("os.name"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "os.version", System.getProperty("os.version"));
        benchApiAddProperty(benchApiProps, benchApiOrigin, "available.processors",
                Runtime.getRuntime().availableProcessors());
        benchApiAddProperty(benchApiProps, benchApiOrigin, "java.max.memory", Runtime.getRuntime().maxMemory());
        benchApiAddProperty(benchApiProps, benchApiOrigin, "deephaven.version", deephavenVersion);
        """;

        ResultTable t = fetchResult(query);

        for (int r = 0, n = t.getRowCount(); r < n; r++) {
            String origin = t.getValue(r, "origin").toString();
            String name = t.getValue(r, "name").toString();
            String value = t.getValue(r, "value").toString();
            benchApiAddProperty(out, origin, name, value);
        }
    }

    // Get the deephaven version either from the dependency or from the uber jar
    private String getDeephavenVersion(Object dhInst) {
        String version = dhInst.getClass().getPackage().getImplementationVersion();
        if (version != null)
            return version;

        URL url = getClass().getResource("/META-INF/maven/deephaven/deephaven-benchmark/pom.xml");
        if (url == null)
            return "Unknown";

        var pom = Filer.getURLText(url);
        return pom.replaceAll("deephaven-java-client-barrage-dagger</artifactId>.*<version>(0.22.0)<", "$1");
    }

    private void benchApiAddProperty(BufferedWriter out, String type, String name, Object value) throws Exception {
        out.write(String.join(",", type, name, value.toString()));
        out.newLine();
    }

}
