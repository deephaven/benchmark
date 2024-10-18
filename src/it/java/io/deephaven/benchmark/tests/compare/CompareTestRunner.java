/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.tests.compare;

import static org.junit.jupiter.api.Assertions.assertTrue;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import io.deephaven.benchmark.api.Bench;
import io.deephaven.benchmark.controller.DeephavenDockerController;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Filer;
import io.deephaven.benchmark.util.Timer;

/**
 * A wrapper for the Bench api that allows running tests for the purpose of comparing Deephaven to other products that
 * perform similar operations. It allows running Deephaven operations or using Deephaven as an agent to run command line
 * python tests in the same environment (e.g. Docker).
 * <p>
 * One of two initializers must be called to set up which type of tests is desired; {@code initDeephaven()} or
 * {@code initPython()}. Deephaven tests run queries inside of Deephaven like the standard benchmarks. Python tests use
 * Deephaven as an agent to run python scripts from the command line by first installing required pip modules in a
 * python virtual environment and then running each test from there.
 * <p>
 * Note: This runner requires test ordering, so it follows that tests in a single test class are meant to be run as a
 * group. This violates the standard Benchmark convention that every test be able to be run by itself. This is done for
 * practical purposes, though it is not ideal.
 */
public class CompareTestRunner {
    final Object testInst;
    final Set<String> requiredPackages = new LinkedHashSet<>();
    final Map<String, String> downloadFiles = new LinkedHashMap<>();
    private Bench api = null;

    public CompareTestRunner(Object testInst) {
        this.testInst = testInst;
    }

    /**
     * Download and place the given file into the environment Deephaven is running in. If the destination directory is
     * specified as a relative path, the download file will be placed relative to the root of the virtual environment
     * this test runner is using for python scripts and pip installs.
     * 
     * @param sourceUri a URI (ex. https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.9/slf4j-api-2.0.9-tests.jar)
     * @param destDir a directory to place the downloaded file into
     */
    public void addDownloadFile(String sourceUri, String destDir) {
        downloadFiles.put(sourceUri, destDir);
    }

    /**
     * Initialize the test as a purely Deephaven test. This should only be called once at the beginning of the test
     * 
     * @param leftTable the main table, which is scalable by row count
     * @param rightTable a fix-size right hand table
     * @param columnNames the columns in both tables to included
     */
    public void initDeephaven(int rowCountFactor, String leftTable, String rightTable, String... columnNames) {
        restartServices();
        generateTable(rowCountFactor, leftTable, columnNames);
        if (rightTable != null)
            generateTable(rowCountFactor, rightTable, columnNames);
        restartServices();
        initialize(testInst);
    }

    /**
     * Require pip to install the given packages into the environment Deephaven is running in. This also activates a
     * mode that uses Deephaven as an test agent to run the python test from the command line
     */
    public void initPython(String... packages) {
        restartDocker(1);
        initialize(testInst);
        requiredPackages.addAll(Arrays.asList(packages));
        if (Arrays.stream(packages).anyMatch(p -> p.startsWith("jdk")))
            requiredPackages.add("install-jdk");
    }

    /**
     * Run a benchmark test, filling in the provided code snippets for each stage. Record the result in the benchmark
     * results csv.
     * 
     * @param name the benchmark name
     * @param setup code to run before the operation
     * @param operation code to run that produces the result
     * @param mainSizeGetter code to get the row size of the table being processed
     * @param resultSizeGetter code to get the row size of the result after the operation
     */
    public void test(String name, String setup, String operation, String mainSizeGetter, String resultSizeGetter) {
        test(name, 0, setup, operation, mainSizeGetter, resultSizeGetter);
    }

    /**
     * Run a benchmark test, filling in the provided code snippets for each stage. Record the result in the benchmark
     * results csv.
     * 
     * @param name the benchmark name
     * @param expectedRowCount the maximum row count expected as a result
     * @param setup code to run before the operation
     * @param operation code to run that produces the result
     * @param mainSizeGetter code to get the row size of the table being processed
     * @param resultSizeGetter code to get the row size of the result after the operation
     */
    public void test(String name, long expectedRowCount, String setup, String operation, String mainSizeGetter,
            String resultSizeGetter) {
        Result result;
        if (requiredPackages.size() > 0) {
            installRequiredPackages();
            result = runPythonTest(name, setup, operation, mainSizeGetter, resultSizeGetter);
        } else {
            result = runDeephavenTest(name, setup, operation, mainSizeGetter, resultSizeGetter);
        }
        var rcount = result.resultRowCount();
        var ecount = (expectedRowCount < 1) ? Long.MAX_VALUE : expectedRowCount;
        assertTrue(rcount > 0 && rcount <= ecount, "Wrong result row count: " + rcount);
        System.out.println("Result Count: " + rcount);
    }

    /**
     * Tell Deephaven to install required python or java packages in its environment. Note: This assumes that after
     * these packages are installed, Deephaven will only be used as an agent to run command line python code
     */
    void installRequiredPackages() {
        var pipPackagesMarker = "--- Bench Pip Installed Versions ---";
        var pipPackages = requiredPackages.stream().filter(p -> !isJdkPackage(p)).toList();

        var query = """
        text = '''PACKAGES='${pipPackages}'
        VENV_PATH=~/deephaven-benchmark-venv
        PIP_INSTALL_VERSIONS=pip-install-versions.txt
        rm -rf ${VENV_PATH}/*
        python3 -m venv ${VENV_PATH}
        cd ${VENV_PATH}
        for PKG in ${PACKAGES}; do
            ./bin/pip install ${PKG}
            ./bin/pip list | grep -E "^${PKG}\s+.*" >> ${PIP_INSTALL_VERSIONS}
        done
        echo "${pipPackagesMarker}"
        cat ${PIP_INSTALL_VERSIONS}
        '''
        save_file('setup-benchmark-workspace.sh', text)
        result = run_script('bash', 'setup-benchmark-workspace.sh')
        
        pip_versions = new_table([
            string_col("versions", [result]),
        ])
        """;
        query = query.replace("${pipPackages}", String.join(" ", pipPackages));
        query = query.replace("${pipPackagesMarker}", pipPackagesMarker);
        api.query(query).fetchAfter("pip_versions", table -> {
            boolean isPastMarker = false;
            for (String line : table.getValue(0, "versions").toString().lines().toList()) {
                line = line.trim();
                if (!isPastMarker && line.equals(pipPackagesMarker)) {
                    isPastMarker = true;
                    continue;
                }
                if (!isPastMarker)
                    continue;

                String[] s = line.split("\\s+");
                if (s.length > 1)
                    api.platform().add("python-dh-agent", "pip." + s[0] + ".version", s[1]);
            }
        }).execute();

        requiredPackages.forEach(p -> installJavaPackage(p));
        downloadFiles.forEach((s, d) -> placeDownloadFile(s, d));
    }

    /**
     * Determine if the given package descriptor has the form of a java package descriptor.
     * 
     * @param javaDescr a package descriptor like 'jdk-11'
     * @return true if the given descriptor describes a java package, otherwise false
     */
    boolean isJdkPackage(String javaDescr) {
        return javaDescr.matches("jdk-[0-9]+");
    }

    /**
     * Download and install a java package according to the descriptor (@see isJdkPackage()) into the virtual
     * environment where python and pip are installed.
     * 
     * @param javaDescr a description like 'jdk-11'
     */
    void installJavaPackage(String javaDescr) {
        if (!isJdkPackage(javaDescr))
            return;
        var version = javaDescr.replaceAll("jdk-([0-9]+)", "$1");
        var query = """
        text = '''
        import os, jdk
        if not os.path.exists('./jdk-${version}'):
            jdk.install('11', vendor='Temurin', path='./')
            os.system('mv jdk*/ jdk-${version}')
            os.system('echo JAVA_HOME=$PWD/jdk-${version} > ENV_VARS.sh')
        '''
        save_file('install-jdk.py', text)
        run_script('./bin/python', 'install-jdk.py')
        """;
        query = query.replace("${version}", String.join(" ", version));
        api.query(query).execute();
    }

    /**
     * Download and place the given source URL to the given destination directory. (@see addDownloadFile()) This method
     * uses python on the Deephaven server to download and place.
     * 
     * @param sourceUri the file to download
     * @param destDir the directory to put the downloaded file in
     */
    void placeDownloadFile(String sourceUri, String destDir) {
        var query = """
        text = '''
        import requests, os

        dest = '${destDir}/' + os.path.basename('${sourceUri}')
        r = requests.get('${sourceUri}', allow_redirects=True)
        open(dest, 'wb').write(r.content)
        '''
        save_file('install-file.py', text)
        run_script('./bin/python', 'install-file.py')
        """;
        query = query.replace("${sourceUri}", sourceUri);
        query = query.replace("${destDir}", destDir);
        api.query(query).execute();
    }


    /**
     * Run the test in Deephaven proper. Do not push to the command line.
     * 
     * @param name the benchmark name
     * @param operation the operation being measured
     * @param mainSizeGetter code to get the row size of the table being processed
     * @param resultSizeGetter code to get the row size of the result after the operation
     * @return the measured result
     */
    Result runDeephavenTest(String name, String setup, String operation, String mainSizeGetter,
            String resultSizeGetter) {
        var query = """
        begin_time = time.perf_counter_ns()
        ${setupQueries}
        ${operation}
        op_duration = time.perf_counter_ns() - begin_time
        
        stats = new_table([
            double_col("elapsed_nanos", [op_duration]),
            long_col("processed_row_count", [${mainSizeGetter}]),
            long_col("result_row_count", [${resultSizeGetter}]),
        ])
        """;
        return runTest(name, query, setup, operation, mainSizeGetter, resultSizeGetter);
    }

    /**
     * Use Deephaven to run the test using the Python command line in a Python virtual environment
     * 
     * @param name the benchmark name
     * @param operation the operation being measured
     * @param mainSizeGetter code to get the row size of the table being processed
     * @param resultSizeGetter code to get the row size of the result after the operation
     * @return the measured result
     */
    Result runPythonTest(String name, String setup, String operation, String mainSizeGetter, String resultSizeGetter) {
        var query = """
        text = '''#!/usr/bin/env bash
        touch ENV_VARS.sh
        source ENV_VARS.sh
        ./bin/python $1
        '''
        save_file('run-benchmark-test.sh', text)
        """;
        api.query(query).execute();

        query = """
        text = '''import time
        ${setupQueries}
        begin_time = time.perf_counter_ns()
        ${operation}
        op_duration = time.perf_counter_ns() - begin_time
        main_size = ${mainSizeGetter}
        result_size = ${resultSizeGetter}

        print("-- Test Results --")
        print("{", "'duration':", op_duration, ", 'main_size':", main_size, ", 'result_size':", result_size, "}")
        '''
        save_file('benchmark-test.py', text)
        result = run_script('./run-benchmark-test.sh', 'benchmark-test.py')
        result = eval(result.splitlines()[-1])
        
        stats = new_table([
            double_col("elapsed_nanos", [result['duration']]),
            long_col("processed_row_count", [result['main_size']]),
            long_col("result_row_count", [result['result_size']])
        ])
        """;
        return runTest(name, query, setup, operation, mainSizeGetter, resultSizeGetter);
    }

    Result runTest(String name, String query, String setup, String operation, String mainSizeGetter,
            String resultSizeGetter) {
        if (api == null)
            throw new RuntimeException("Initialize with initDeephaven() or initPython()s before running the test");
        api.setName(name);
        stopUnusedServices();
        query = query.replace("${setupQueries}", setup);
        query = query.replace("${operation}", operation);
        query = query.replace("${mainSizeGetter}", mainSizeGetter);
        query = query.replace("${resultSizeGetter}", resultSizeGetter);

        try {
            var result = new AtomicReference<Result>();
            api.query(query).fetchAfter("stats", table -> {
                long loadedRowCount = table.getSum("processed_row_count").longValue();
                long resultRowCount = table.getSum("result_row_count").longValue();
                long elapsedNanos = table.getSum("elapsed_nanos").longValue();
                var r = new Result(loadedRowCount, Duration.ofNanos(elapsedNanos), resultRowCount);
                result.set(r);
            }).execute();
            api.result().test("deephaven-engine", result.get().elapsedTime(), result.get().loadedRowCount());
            return result.get();
        } finally {
            api.close();
        }
    }

    void initialize(Object testInst) {
        var query = """
        import subprocess, os, stat, time
        from pathlib import Path
        from deephaven import new_table, garbage_collect
        from deephaven.column import long_col, double_col, string_col

        user_home = str(Path.home())
        benchmark_home = user_home + '/deephaven-benchmark-venv'
        
        def save_file(file_name, file_text):
            os.makedirs(benchmark_home, exist_ok=True)
            file_path = benchmark_home + '/' + file_name
            with open(file_path, 'w') as f:
                f.write(file_text)
            st = os.stat(file_path)
            os.chmod(file_path, st.st_mode | stat.S_IEXEC)

        def run_script(runner, script_name):
            command_array = [runner, script_name]
            output=subprocess.check_output(command_array, cwd=benchmark_home).decode("utf-8")
            print(output)
            return output
        """;
        api = Bench.create(testInst);
        api.query(query).execute();
    }

    void restartServices() {
        var api = Bench.create("# Services Restart");
        try {
            api.setName("# Services Restart");
            var c = new DeephavenDockerController(api.property("docker.compose.file", ""),
                    api.property("deephaven.addr", ""));
            c.restartService();
        } finally {
            api.close();
        }
    }

    void restartDocker(int heapGigs) {
        var api = Bench.create("# Services Restart");
        try {
            api.setName("# Services Restart " + heapGigs + "G");
            String dockerComposeFile = api.property("docker.compose.file", "");
            String deephavenHostPort = api.property("deephaven.addr", "");
            if (dockerComposeFile.isBlank() || deephavenHostPort.isBlank())
                return;
            dockerComposeFile = makeHeapAdjustedDockerCompose(dockerComposeFile, heapGigs);
            var controller = new DeephavenDockerController(dockerComposeFile, deephavenHostPort);
            controller.restartService();
        } finally {
            api.close();
        }
    }

    void stopUnusedServices() {
        var timer = api.timer();
        var c = new DeephavenDockerController(api.property("docker.compose.file", ""), api.property("deephaven.addr", ""));
        if (!c.stopService(Set.of("deephaven")))
            return;
        var metrics = new Metrics(Timer.now(), "test-runner", "setup.services");
        metrics.set("stop", timer.duration().toMillis(), "standard");
        api.metrics().add(metrics);
    }

    // Replace heap (e.g. -Xmx64g) in docker-compose.yml with new heap value
    String makeHeapAdjustedDockerCompose(String dockerComposeFile, int heapGigs) {
        Path sourceComposeFile = Paths.get(dockerComposeFile);
        String newComposeName = sourceComposeFile.getFileName().toString().replace(".yml", "." + heapGigs + "g.yml");
        Path destComposeFile = sourceComposeFile.resolveSibling(newComposeName);
        String composeText = Filer.getFileText(sourceComposeFile);
        composeText = composeText.replaceAll("[-]Xmx[0-9]+[gG]", "-Xmx" + heapGigs + "g");
        Filer.putFileText(destComposeFile, composeText);
        return destComposeFile.toString();
    }

    long getScaleRowCount(Bench api, int rowCountFactor) {
        return (long) (api.propertyAsIntegral("scale.row.count", "100000") * rowCountFactor);
    }

    void generateTable(int rowCountFactor, String tableName, String... columnNames) {
        Bench api = Bench.create("# Generate Table");
        try {
            api.setName("# Generate Table");
            switch (tableName) {
                case "source" -> generateSourceTable(api, rowCountFactor, columnNames);
                case "right" -> generateRightTable(api, columnNames);
                default -> throw new RuntimeException("Undefined table name: " + tableName);
            }
        } finally {
            api.close();
        }
    }

    void generateSourceTable(Bench api, int rowCountFactor, String... columnNames) {
        var table = api.table("source");
        for (String columnName : columnNames) {
            switch (columnName) {
                case "int250" -> table.add("int250", "int", "[1-250]");
                case "int640" -> table.add("int640", "int", "[1-640]");
                case "int1M" -> table.add("int1M", "int", "[1-1000000]");
                case "str250" -> table.add("str250", "string", "[1-250]");
            }
        }
        table.withCompression("snappy");
        table.withRowCount(getScaleRowCount(api, rowCountFactor));
        table.generateParquet();
    }

    void generateRightTable(Bench api, String... columnNames) {
        var table = api.table("right").withDefaultDistribution("ascending");
        for (String columnName : columnNames) {
            switch (columnName) {
                case "r_str250" -> table.add("r_str250", "string", "[1-250]");
                case "r_int1M" -> table.add("r_int1M", "int", "[1-1000000]");
            }
        }
        table.withCompression("snappy");
        table.generateParquet();
    }

    record Result(long loadedRowCount, Duration elapsedTime, long resultRowCount) {
    }

}
