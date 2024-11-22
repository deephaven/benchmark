/* Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

/**
 * Contains snippets of python functions that can be called inside a query executed on the Deephaven Engine
 */
class Snippets {
    /**
     * Provides a consumer to a kafka topic according to the APIs properties (e.g. kafka.consumer.addr)
     * <p>
     * ex. mytable = bench_api_kafka_consume('mytopic', 'append')
     * 
     * @param topic a kafka topic name
     * @param table_type a Deephaven table type <code>( append | blink | ring )</code>
     * @return a table that is populated with the rows from the topic
     */
    static String bench_api_kafka_consume = """
        from deephaven.stream.kafka import consumer as kc
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
     * <p>
     * ex. bench_api_await_table_size(table, 1000000)
     * 
     * @param table the table to monitor
     * @param row_count the number of rows to wait for
     */
    static String bench_api_await_table_size = """
        from deephaven.table import Table
        from deephaven.update_graph import exclusive_lock

        def bench_api_await_table_size(table: Table, row_count: int):
            with exclusive_lock(table):
                while table.j_table.size() < row_count:
                    table.j_table.awaitUpdate()
        """;


    /**
     * Captures the value of the first column in a table every Deephaven ticking interval and does not allow advancement
     * in the current query logic until that value is reached
     * <p>
     * ex. bench_api_await_column_value_limit(table, 'count', 1000000)
     * 
     * @param table the table to monitor
     * @param column the column name to monitor
     * @param limit the upper bound for the monitored column value
     */
    static String bench_api_await_column_value_limit = """
        from deephaven.table import Table
        from deephaven.update_graph import exclusive_lock
        def bench_api_await_column_value_limit(table: Table, column: str, limit: int):
            with exclusive_lock(table):
                value = 0
                while value < limit:
                    table.j_table.awaitUpdate()
                    value = table.j_object.getColumnSource(column).get(0)
        """;

    /**
     * Initialize the container for storing benchmark metrics. Define functions for getting some MX Bean data for gc,
     * jit and heap
     * <p>
     * ex. bench_api_metrics_init()
     */
    static String bench_api_metrics_init = """
        def bench_api_metrics_init():
            global bench_api_metrics
            bench_api_metrics = []
        """;

    /**
     * Get the MX bean for the given getter factory method that works from
     * <code>java.lang.management.ManagementFactory</code>
     */
    static String bench_api_get_bean = """
        import jpy
        def bench_api_get_bean(bean_getter):
            return getattr(jpy.get_type('java.lang.management.ManagementFactory'),bean_getter)()
        """;

    /**
     * Get the current JVM heap usage in bytes
     */
    static String bench_api_mem_usage = """
        def bench_api_mem_usage():
            return bench_api_get_bean('getMemoryMXBean').getHeapMemoryUsage().getUsed()
        """;

    /**
     * Get the accumulated compile time
     */
    static String bench_api_compile_time = """
        def bench_api_compile_time():
            return bench_api_get_bean('getCompilationMXBean').getTotalCompilationTime()
        """;

    /**
     * Get the accumulated total time spent in GC and GC count
     */
    static String bench_api_gc_info = """
        def bench_api_gc_info():
            total = 0.0; count = 0
            beans = bench_api_get_bean('getGarbageCollectorMXBeans')
            for i in range(0, beans.size()):
                b = beans.get(i)
                total = total + b.getCollectionTime()
                count = count + b.getCollectionCount()
            return total, count
        """;

    /**
     * Set heap usage, compile time, GC time and GC Count to global variables
     */
    static String bench_api_metrics_start = """
        from deephaven import garbage_collect   
        def bench_api_metrics_start():
            global bench_mem_usage, bench_compile_time, bench_gc_time, bench_gc_count
            garbage_collect()
            bench_compile_time = bench_api_compile_time()
            bench_gc_time, bench_gc_count = bench_api_gc_info()
            bench_mem_usage = bench_api_mem_usage()
        """;

    /**
     * Get difference from <code>bench_api_metrics_start</code> values and add as collected metrics
     */
    static String bench_api_metrics_end = """
        def bench_api_metrics_end():
            bench_api_metrics_add('operation','compile.time',(bench_api_compile_time()-bench_compile_time)/1000.0)
            gc_time, gc_count = bench_api_gc_info()
            bench_api_metrics_add('operation','gc.time',(gc_time - bench_gc_time)/1000.0)
            bench_api_metrics_add('operation','gc.count',gc_count - bench_gc_count)
            garbage_collect()
            bench_api_metrics_add('operation','heap.gain',bench_api_mem_usage() - bench_mem_usage) 
        """;

    /**
     * Add a metrics to the accumulated list of metrics that will be transformed by
     * <code>bench_api_metrics_collect</code> into a Deephaven table for retrieval
     * <p>
     * ex. bench_api_metrics_add('docker', 'restart.secs', '5.1', 'restart duration in between tests')
     * 
     * @param category the metric category
     * @param name the name of the metric
     * @param value the number value for the metric
     * @param note an optional short description for context
     */
    static String bench_api_metrics_add = """
        import time
        def bench_api_metrics_add(category, name, value, note=''):
            now_millis = int(time.time() * 1000)
            bench_api_metrics.append([now_millis, 'deephaven-engine', category, name, value, note])
        """;

    /**
     * Collect any metrics and turn them into a Deephaven table that can be fetched from the bench api.
     * <p>
     * ex. bench_api_metrics_table = bench_api_metrics_collect()
     */
    static String bench_api_metrics_collect = """
        from deephaven import input_table, empty_table, dtypes as dht
        def bench_api_metrics_collect():
            s = dht.string
            t = input_table({'timestamp':s,'origin':s,'category':s,'name':s,'value':s,'note':s})
            for m in bench_api_metrics:
                m1 = empty_table(1).update(['timestamp=``+m[0]','origin=``+m[1]','category=``+m[2]',
                    'name=``+m[3]','value=``+m[4]','note=``+m[5]'])    
                t.add(m1)
            return t
        """;

    /**
     * Returns a query containing the api functions called by the query
     * 
     * @param query the query containing called functions
     * @return a query containing function definitions
     */
    static String getFunctions(String query) {
        String defs = "";
        defs += getFunc("bench_api_kafka_consume", bench_api_kafka_consume, query, "");
        defs += getFunc("bench_api_await_table_size", bench_api_await_table_size, query, defs);
        defs += getFunc("bench_api_metrics_init", bench_api_metrics_init, query, defs);
        defs += getFunc("bench_api_metrics_start", bench_api_metrics_start, query, defs);
        defs += getFunc("bench_api_metrics_end", bench_api_metrics_end, query, defs);
        defs += getFunc("bench_api_mem_usage", bench_api_mem_usage, query, defs);
        defs += getFunc("bench_api_compile_time", bench_api_compile_time, query, defs);
        defs += getFunc("bench_api_gc_info", bench_api_gc_info, query, defs);
        defs += getFunc("bench_api_get_bean", bench_api_get_bean, query, defs);
        defs += getFunc("bench_api_metrics_add", bench_api_metrics_add, query, defs);
        defs += getFunc("bench_api_metrics_collect", bench_api_metrics_collect, query, defs);
        defs += getFunc("bench_api_await_column_value_limit", bench_api_await_column_value_limit, query, defs);
        return defs;
    }

    static String getFunc(String functionName, String functionDef, String query, String funcs) {
        if (!query.contains(functionName) && !funcs.contains(functionName))
            return "";
        return functionDef + System.lineSeparator();
    }

}
