package io.deephaven.benchmark.tests.compare;

public class Setup {
    static public String flink(CompareTestRunner r) {
        addDownloadJar(r, "flink", "flink-sql-parquet", "1.17.1");
        addDownloadJar(r, "flink", "flink-oss-fs-hadoop", "1.17.1");
        addDownloadJar(r, "hadoop", "hadoop-mapreduce-client-core", "2.10.2");
        
        var q = """
        import time, os
        import pandas as pd
        from pyflink.common import Row
        from pyflink.table import (EnvironmentSettings, TableEnvironment)
        from pyflink.table.expressions import lit, col
        from pyflink.table.udf import udtf

        t_env = TableEnvironment.create(EnvironmentSettings.in_batch_mode())
        t_env.get_config().set("parallelism.default", "1")
        t_env.get_config().set("taskmanager.memory.process.size", "24g")
        t_env.get_config().set("jobmanager.memory.process.size", "24g")
        t_env.get_config().set("python.fn-execution.arrow.batch.size", "10000000")
        t_env.get_config().set("python.fn-execution.bundle.size", "10000000")
        t_env.get_config().set("python.state.cache-size", "10000000")
        t_env.get_config().set("python.map-state.iterate-response-batch-size", "10000000")
        t_env.get_config().set("python.map-state.read-cache-size", "10000000")
        t_env.get_config().set("python.map-state.write-cache-size", "10000000")
        t_env.get_config().set("python.metric.enabled", "false")
        t_env.get_config().set("python.operator-chaining.enabled", "true")
        
        os.system('rm -rf /data/results.csv')

        def count_rows(table_name):
            sname = table_name + '_stats'
            stats_dir = '/data/' + sname + '.csv'
            os.system('rm -rf ' + stats_dir)
            t_env.execute_sql("CREATE TABLE " + sname + "(row_count BIGINT) WITH ('connector'='filesystem','path'='" + stats_dir + "','format'='csv')")
            t_env.execute_sql("INSERT INTO " + sname + " SELECT count(*) AS row_count FROM " + table_name).wait()
            count = 0
            for r in t_env.from_path(sname).execute().collect():
                count = r[0]
            return count
        """;
        return q;
    }
    
    static public void addDownloadJar(CompareTestRunner r, String prod, String artifact, String version) {
        var destDir = "lib/python3.10/site-packages/pyflink/lib";
        var apacheUri = "https://repo1.maven.org/maven2/org/apache/";
        var uri = apacheUri + prod + '/' + artifact + '/' + version + '/' + artifact + '-' + version + ".jar";
        r.addDownloadFiles(uri, destDir);
    }
}
