/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.BufferedWriter;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.engine.exceptions.ArgumentException;

/**
 * Collects various properties about the running client and server used during a benchmark run and stores them in the
 * benchmark results directory. Since properties are potentially collected from multiple tests, and there is a single
 * platform property file for an entire test run, properties, once added, are not permitted to be overwritten.
 */
public class BenchPlatform {
    static final Map<String, Property> properties = new LinkedHashMap<>();
    static boolean hasBeenCommitted = false;
    final Path platformFile;
    final Properties profileProps;


    /**
     * Initialize platform detail collection with the default result file name.
     * 
     * @param parent the parent directory of the platform file
     */
    BenchPlatform(Path parent) {
        this(parent, Bench.platformFileName, Bench.profile.getProperties());
    }

    /**
     * Initialize platform detail collection using the given result file name
     * 
     * @param parent the parent directory of the platform file
     * @param platformFileName the name the file to store platform properties
     */
    BenchPlatform(Path parent, String platformFileName, Properties profileProps) {
        this.platformFile = parent.resolve(platformFileName);
        this.profileProps = profileProps;
    }

    /**
     * Add a platform detail
     * 
     * @param origin where the platform detail came from
     * @param name name of the platform detail
     * @param value value of the platform detail
     * @return this instance
     */
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
            addTestRunnerProps(properties);
            addRunnerProfileProps(properties);
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
        api.query(query).fetchAfter("bench_api_platform", table -> {
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

    private void addRunnerProfileProps(Map<String, Property> benchApiProps) {
        var origin = "test-runner";
        profileProps.forEach((k, v) -> {
            var name = k.toString();
            var value = maskSecrets(name, v.toString());
            benchApiAddProperty(benchApiProps, origin, name, value);
        });
    }

    private String maskSecrets(String name, String value) {
        name = name.toLowerCase();
        if (name.contains("token") || name.contains("channel")) {
            value = value.substring(0, Math.min(value.length(), 5)).replaceAll(".", "*");
        }
        return value;
    }

    private void addTestRunnerProps(Map<String, Property> benchApiProps) {
        var dhInst = new ArgumentException();
        var benchApiOrigin = "test-runner";
        var deephavenVersion = getDeephavenVersion(dhInst, "/META-INF/maven/io.deephaven/deephaven-benchmark/pom.xml");

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
        import jpy, sys, os, re
        from deephaven import new_table, input_table, dtypes as dht, perfmon as pm
        from deephaven.column import string_col as sc
        
        Runtime = jpy.get_type('java.lang.Runtime')
        bench_api_platform = input_table({'origin':dht.string, 'name':dht.string, 'value':dht.string})
        
        def bench_api_add_platform(name, value):
            name = str(name).strip(); value = str(value).strip()
            t = new_table([sc('origin', ['deephaven-engine']), sc('name', [name]), sc('value', [value])])
            bench_api_platform.add(t)
        
        def bench_api_get_proc_info(proctype, prockey):
            info = pm.process_info_log().where(['Type.equals(proctype)','Key.equals(prockey)'])
            return info.j_table.columnIterator('Value').next()
        
        def bench_api_add_proc_info(name, proctype, prockeys, delim=' '):
            values = []
            for prockey in prockeys:
                value = bench_api_get_proc_info(proctype, prockey)
                values.append(value)
            bench_api_add_platform(name, delim.join(values))
        
        def bench_api_add_proc_args(name, proctype, len_name, includes=(), excludes=()):
            prockeys = []
            for i in range(int(bench_api_get_proc_info(proctype,len_name))):
                value = bench_api_get_proc_info('runtime-mx.jvm-args',str(i))
                if(value.startswith(includes) and not value.startswith(excludes)):
                    prockeys.append(str(i))
            bench_api_add_proc_info(name, proctype, prockeys)
        
        bench_api_add_proc_info('system.os.version','system-info.os',['family','version.version','version.name'])
        bench_api_add_proc_info('system.kernel.version','system-info.os',['version.build'])
        bench_api_add_proc_info('system.manufacturer','system-info.sys',['manufacturer','model'])
        bench_api_add_proc_info('system.physical.memory','system-info.memory',['physical'])
        bench_api_add_proc_info('system.baseboard.maker','system-info.sys',['baseboard.manufacturer','baseboard.model'])
        bench_api_add_proc_info('system.cpu.model','system-info.cpu',['name'])
        bench_api_add_proc_info('system.cpu.cores','system-info.cpu',['physical','logical'],'/')
        bench_api_add_proc_info('java.version','runtime-mx.sys-props',['java.vendor.version'])
        bench_api_add_proc_info('java.max.heap','memory-mx.heap',['max'])
        bench_api_add_platform('java.available.processors',Runtime.getRuntime().availableProcessors())
        bench_api_add_proc_args('java.xx.args','runtime-mx.jvm-args','len',('-XX'),('-XX:CompilerDirectivesFile=','-XX:+UnlockD'))
        dhInst = jpy.get_type('io.deephaven.engine.exceptions.ArgumentException')()
        deephaven_version = dhInst.getClass().getPackage().getImplementationVersion()
        bench_api_add_platform('deephaven.version', deephaven_version)
        python_version = '.'.join([str(sys.version_info.major), str(sys.version_info.minor), str(sys.version_info.micro)])
        bench_api_add_platform('python.version', python_version)
        
        # Java Dependency Versions
        classpath = bench_api_get_proc_info('runtime-mx.sys-props','java.class.path')
        from collections import defaultdict
        jar_versions = defaultdict(list)
        total_jar_size = 0
        engine_artifact=None
        for f in sorted(re.split(':|;|,',classpath)):
            artifact_full = os.path.basename(f)
            if re.search('deephaven-engine-api-.*[.]jar',artifact_full):
                engine_artifact = os.path.abspath(f)
            if(os.path.exists(f)):
                total_jar_size = total_jar_size + os.path.getsize(f)
                artifact_name = re.sub('[-][0-9]+[.].*jar$','',artifact_full)
                artifact_vers = re.sub('(^-)|([.]jar$)','',artifact_full.replace(artifact_name,''))
                if not (artifact_name.startswith('deephaven-') and artifact_vers == deephaven_version):
                    jar_versions[artifact_name + '.jar'].append(artifact_vers)
        
        for key, value in jar_versions.items():
            value.sort(key=lambda x: x.count('.'))
            bench_api_add_platform(key,' : '.join(value))
        bench_api_add_platform('dependency.jar.size',total_jar_size)
        
        # Get the class version Deephaven was compiled with
        engine_class='io/deephaven/engine/table/Table'
        command=f'javap -cp {engine_artifact} -v {engine_class} | grep -E "(minor .*: [0-9]+)|(major .*: [0-9]+)" | sort'
        class_vers = '.'.join(re.findall('[0-9]+',os.popen(command).read()))
        bench_api_add_platform('java.class.version',class_vers)
        
        # Python Dependency Versions
        import importlib.metadata
        package_dists = {}
        package_locations = {}
        for dist in sorted(importlib.metadata.distributions(), key=lambda x: x.name):
            if(os.path.basename(dist.locate_file('')) == 'site-packages'):
                package_dists[dist.name] = dist.version
                package_locations[dist.locate_file('')] = None
        
        for key, value in sorted(package_dists.items()):
            bench_api_add_platform(key + '.py', value)
        
        from pathlib import Path
        total_python_size = 0
        for location in package_locations:
            total_python_size = sum(p.stat().st_size for p in Path(location).rglob('*'))
        
        bench_api_add_platform('dependency.python.size',total_python_size)
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
        var v = value.toString();
        var prop = new Property(origin, name, v, new AtomicBoolean(false));
        properties.putIfAbsent(prop.getName(), prop);
    }

    static void writeLine(Property prop, Path file) {
        try (BufferedWriter out = Files.newBufferedWriter(file, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            out.write(String.join(",", normalize(prop.origin()), normalize(prop.name()), normalize(prop.value())));
            out.newLine();
        } catch (Exception ex) {
            throw new RuntimeException("Failed to write result to file: " + file, ex);
        }
    }

    static String normalize(String value) {
        return value.replaceAll("[\"',]", " ").replaceAll("\\s+", " ").trim();
    }

    record Property(String origin, String name, String value, AtomicBoolean isWritten) {
        String getName() {
            return origin + ">>" + name;
        }
    }

}
