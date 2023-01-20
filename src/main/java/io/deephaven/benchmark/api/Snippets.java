package io.deephaven.benchmark.api;

/**
 * Contains snippets of query code that can be called inside a query
 */
class Snippets {
	/**
	 * Provides a consumer to a kafka topic according to the APIs properties (e.g. kafka.consumer.addr)<p/>
	 * ex. mytable = bench_api_kafka_consume('mytopic', 'append')
	 * @param topic a kafka topic name
	 * @param table_type a Deephaven table type <code>( append | stream | ring )</code> 
	 * @return a table that is populated with the rows from the topic
	 */
	static String bench_api_kafka_consume = 
		"""
		from deephaven import kafka_consumer as kc
		from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
			
		def bench_api_kafka_consume(topic: str, table_type: str):
			t_type = None
			if table_type == 'append': t_type = TableType.append()
			elif table_type == 'stream': t_type = TableType.stream()
			elif table_type == 'ring': t_type = TableType.ring()
			else: raise Exception('Unsupported kafka stream type: {}'.format(t_type))
				
			return kc.consume(
				{ 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
				topic, partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
				key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec(topic + '_record', schema_version='1'),
				table_type=t_type)
		""";
	
	static String bench_api_await_table_size = 
		"""
		from deephaven.table import Table
		from deephaven.ugp import exclusive_lock

		def bench_api_await_table_size(table: Table, row_count: int):
			with exclusive_lock():
				while table.j_table.size() < row_count:
					table.j_table.awaitUpdate()
		""";
	
	/**
	 * Returns a query containing the api functions called by the query
	 * @param query the query containing called functions
	 * @return a query containing function definitions
	 */
	static String getFunctions(String query) {
		String functionDefs = "";
		functionDefs += getFunction("bench_api_kafka_consume", bench_api_kafka_consume, query);
		functionDefs += getFunction("bench_api_await_table_size", bench_api_await_table_size, query);
		return functionDefs;
	}
	
	static String getFunction(String functionName, String functionDef, String query) {
		return query.contains(functionName)?(functionDef+System.lineSeparator()):"";
	}

}
