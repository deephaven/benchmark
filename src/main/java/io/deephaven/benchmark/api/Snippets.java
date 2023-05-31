/* Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

/**
 * Contains snippets of query code that can be called inside a query
 */
class Snippets {
    /**
     * Provides a consumer to a kafka topic according to the APIs properties (e.g. kafka.consumer.addr)
     * <p/>
     * ex. mytable = bench_api_kafka_consume('mytopic', 'append')
     * 
     * @param topic a kafka topic name
     * @param table_type a Deephaven table type <code>( append | blink | ring )</code>
     * @return a table that is populated with the rows from the topic
     */
    static String bench_api_kafka_consume = """
        from deephaven import kafka_consumer as kc
        from deephaven.stream.kafka.consumer import TableType, KeyValueSpec

        def bench_api_kafka_consume(topic: str, table_type: str):
            t_type = None
            if table_type == 'append': t_type = TableType.append()
            elif table_type == 'blink': t_type = TableType.blink()
            elif table_type == 'ring': t_type = TableType.ring()
            else: raise Exception('Unsupported kafka stream type: {}'.format(t_type))

            return kc.consume(
                { 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
                topic, partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
                key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec(topic + '_record', schema_version='1'),
                table_type=t_type)
        """;

    /**
     * Captures table size every Deephaven ticking interval and does not allow advancement in the current query logic
     * until the given table size is reached
     * <p/>
     * ex. bench_api_await_table_size(table, 1000000)
     * 
     * @param table the table to monitor
     * @param row_count the number of rows to wait for
     */
    static String bench_api_await_table_size = """
        from deephaven.table import Table
        from deephaven.ugp import exclusive_lock

        def bench_api_await_table_size(table: Table, row_count: int):
            with exclusive_lock():
                while table.j_table.size() < row_count:
                    table.j_table.awaitUpdate()
        """;

    /**
     * Take a snapshot of a selection of JVM statistics. Multiple snapshots can be made in one query with each producing
     * a set of metrics that can be collected by <code>bench_api_metrics_collect</code>
     */
    static String bench_api_metrics_snapshot = """
        import jpy, time
        bench_api_metrics = []
        def bench_api_metrics_snapshot():
            millis = int(time.time() * 1000)
            MxMan = jpy.get_type("java.lang.management.ManagementFactory")
            def add(bean, getters: [], notes = ''):
                for getter in getters:
                    nameFunc = getattr(bean, 'getName') if hasattr(bean, 'getName') else getattr(bean, 'getObjectName')
                    valFunc = getattr(bean, 'get' + getter)
                    row = [str(millis), bean.getClass().getSimpleName(), str(nameFunc()), getter, str(valFunc()), notes]
                    bench_api_metrics.append(row)
                    
            add(MxMan.getClassLoadingMXBean(), ['TotalLoadedClassCount', 'UnloadedClassCount'])
            add(MxMan.getMemoryMXBean(), ['ObjectPendingFinalizationCount', 'HeapMemoryUsage', 'NonHeapMemoryUsage'])
            add(MxMan.getThreadMXBean(), ['ThreadCount', 'PeakThreadCount'])
            add(MxMan.getCompilationMXBean(), ['TotalCompilationTime'])
            add(MxMan.getOperatingSystemMXBean(), ['SystemLoadAverage'])
            pools = MxMan.getMemoryPoolMXBeans()
            for i in range(0, pools.size()):
                add(pools.get(i), ['Usage'], str(pools.get(i).getType()))
            gcs = MxMan.getGarbageCollectorMXBeans()
            for i in range(0, gcs.size()):
                add(gcs.get(i), ['CollectionCount', 'CollectionTime'])
        """;

    /**
     * Collect any snapshots of metrics and turn them into a Deephaven table that can be fetched from the bench api.
     * <p/>
     * ex. bench_api_metrics_table = bench_api_collect()
     */
    static String bench_api_metrics_collect = """
        from deephaven import new_table
        from deephaven.column import string_col
        def bench_api_metrics_collect():
            timestamps = []; origins = []; beans = []; types = []
            names = []; values = []; notes = []
            for m in bench_api_metrics:
                timestamps.append(m[0])
                origins.append('deephaven-engine')
                beans.append(m[1])
                types.append(m[2])
                names.append(m[3])
                values.append(m[4])
                notes.append(m[5])
        
            return new_table([
                string_col('timestamp', timestamps),
                string_col('origin', origins),
                string_col('category', beans),
                string_col('type', types),
                string_col('name', names),
                string_col('value', values),
                string_col('note', notes)
            ])
        """;

    /**
     * Returns a query containing the api functions called by the query
     * 
     * @param query the query containing called functions
     * @return a query containing function definitions
     */
    static String getFunctions(String query) {
        String functionDefs = "";
        functionDefs += getFunction("bench_api_kafka_consume", bench_api_kafka_consume, query);
        functionDefs += getFunction("bench_api_await_table_size", bench_api_await_table_size, query);
        functionDefs += getFunction("bench_api_metrics_snapshot", bench_api_metrics_snapshot, query);
        functionDefs += getFunction("bench_api_metrics_collect", bench_api_metrics_collect, query);
        return functionDefs;
    }

    static String getFunction(String functionName, String functionDef, String query) {
        return query.contains(functionName) ? (functionDef + System.lineSeparator()) : "";
    }

}
