/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Numbers;
import io.deephaven.engine.exceptions.ArgumentException;

/**
 * Collects various properties about the running client and server used during a benchmark run and stores them in the
 * benchmark results directory. Since properties are potentially collected from multiple tests, and there is a single
 * platform property file for an entire test run, properties, once added, are not permitted to be overwritten.
 */
public class BenchPlatform {
    static final String platformFileName = "benchmark-platform.csv";
    static final Map<String, Property> properties = new LinkedHashMap<>();
    static boolean hasBeenCommitted = false;
    final Path platformFile;


    /**
     * Initialize platform detail collection with the default result file name.
     * 
     * @param parent the parent directory of the platform file
     */
    BenchPlatform(Path parent) {
        this(parent, platformFileName);
    }

    /**
     * Initialize platform detail collection using the given result file name
     * 
     * @param parent the parent directory of the platform file
     * @param platformFileName the name the file to store platform properties
     */
    BenchPlatform(Path parent, String platformFileName) {
        this.platformFile = parent.resolve(platformFileName);
    }

    public BenchPlatform add(String origin, String name, Object value) {
        benchApiAddProperty(properties, origin, name, value);
        return this;
    }

    /**
     * Ensure that collected platform properties have been saved
     */
    void commit() {
        if (!hasBeenCommitted) {
            hasBeenCommitted = true;
            Filer.delete(platformFile);
            writeLine(new Property("origin", "name", "value", new AtomicBoolean(true)), platformFile);
            addTestProps(properties);
            addEngineProps(properties);
        }
        for (Property prop : properties.values()) {
            if (!prop.isWritten().get()) {
                writeLine(prop, platformFile);
                prop.isWritten().set(true);
            }
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

    /**
     * Get the Deephaven version either by a class implementation or a pom dependency, whichever is available
     * 
     * @param dhInst any deephaven engine object the has implementation-version in its package
     * @param pomResource a resource to a pom found in the classpath
     * @return the version or Unknown
     */
    String getDeephavenVersion(Object dhInst, String pomResource) {
        String version = dhInst.getClass().getPackage().getImplementationVersion();
        if (version != null)
            return version;

        URL url = getClass().getResource(pomResource);
        if (url == null)
            return "Unknown";

        var pom = Filer.getURLText(url);
        var v = pom.replaceAll("(?s).*>deephaven-java-client-barrage-dagger</artifactId>\\s+<version>([^<]+)<.*", "$1");
        return v.matches("[0-9]+\\.[0-9]+\\.[0-9]+") ? v : "Unknown";
    }

    private void addTestProps(Map<String, Property> benchApiProps) {
        var dhInst = new ArgumentException();
        var benchApiOrigin = "test-runner";
        var deephavenVersion = getDeephavenVersion(dhInst, "/META-INF/maven/io.deephaven/deephaven-benchmark/pom.xml");

        // Java Properties (These match the Python calls in addEngineProps
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

    private void addEngineProps(Map<String, Property> benchApiProps) {
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
            benchApiAddProperty(properties, origin, name, value);
        }
    }

    private void benchApiAddProperty(Map<String, Property> properties, String origin, String name, Object value) {
        var v = formatValue(name, value);
        var prop = new Property(origin, name, v, new AtomicBoolean(false));
        properties.putIfAbsent(prop.getName(), prop);
    }

    static void writeLine(Property prop, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write(String.join(",", prop.origin(), prop.name(), prop.value()));
            out.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write result to file: " + file, ex);
        }
    }

    private String formatValue(String name, Object value) {
        switch (name) {
            case "java.max.memory":
                return Numbers.formatBytesToGigs(value);
            default:
                return value.toString();
        }
    }

    record Property(String origin, String name, String value, AtomicBoolean isWritten) {
        String getName() {
            return origin + ">>" + name;
        }
    }

}
