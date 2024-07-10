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
     * <p/>
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
     * <p/>
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
     * Initialize the container for storing benchmark metrics
     * <p/>
     * ex. bench_api_metrics_init()
     */
    static String bench_api_metrics_init = """
        def bench_api_metrics_init():
            global bench_api_metrics
            bench_api_metrics = []
        """;

    /**
     * Captures the value of the first column in a table every Deephaven ticking interval and does not allow advancement
     * in the current query logic until that value is reached
     * <p/>
     * ex. bench_api_metrics_add('docker', 'restart.secs', 5.1, 'restart duration in between tests')
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
     * <p/>
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
        String functionDefs = "";
        functionDefs += getFunction("bench_api_kafka_consume", bench_api_kafka_consume, query);
        functionDefs += getFunction("bench_api_await_table_size", bench_api_await_table_size, query);
        functionDefs += getFunction("bench_api_metrics_init", bench_api_metrics_init, query);
        functionDefs += getFunction("bench_api_metrics_add", bench_api_metrics_add, query);
        functionDefs += getFunction("bench_api_metrics_collect", bench_api_metrics_collect, query);
        functionDefs += getFunction("bench_api_await_column_value_limit", bench_api_await_column_value_limit, query);
        return functionDefs;
    }

    static String getFunction(String functionName, String functionDef, String query) {
        return query.contains(functionName) ? (functionDef + System.lineSeparator()) : "";
    }

}
