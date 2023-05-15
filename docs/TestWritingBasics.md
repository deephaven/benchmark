# Test-writing Basics

## Entry into the Benchmark API
A Bench API instance allows configuration for test data generation, execution of queries against the Deephaven Engine, and state for test metrics.
````
public class AvroKafkaJoinStream {
	Bench api = Bench.create(this);
}
````
There is no need to memorize a class structure for the API.  Everthing starts from a Bench instance and can be followed using "." with code insight.

## Table Generation
Table data can be produced by defining columns, types, and sample data.
````
api.table("stock_trans").random()
	.add("symbol", "string", "SYM[1-1000]")
	.add("price", "float", "[100-200]")
	.add("buys", "int", "[1-100]")
	.add("sells", "int", "[1-100]")
	.generateParquet();
````
This generates a parquet file on the server with the path *data/stock_trans.parquet* for use in subsequent queries.

### Table Types:
- random: Will choose random values for each column range inclusive of the boundaries
- fixed: Will iterate through the range inclusive of the boundaries.  Table row count will be the longest range.

### Column Types:
- string
- long
- int
- double
- float

### Generator Types:
- Parquet: Generates a parquet file to the Deephaven Engines data directory in ZSTD compressed format
- Avro: Generates records to a Kafka topic using Avro serializers and spec

## Query Scripts
Table data can be queried from a Parquet file as in the following query script:
````
from deephaven.parquet import read

p_stock_trans = read('/data/stock_trans.parquet')	
````
Or it can queried from a Kafka topic as in the following:
````
from deephaven import kafka_consumer as kc
from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
		
kafka_stock_trans = kc.consume(
	{ 'bootstrap.servers' : '${kafka.consumer.addr}', 'schema.registry.url' : 'http://${schema.registry.addr}' },
	'stock_trans', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
	key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('stock_trans_record', schema_version='1'),
	table_type=TableType.append())
````
Examples of using Deephaven query scripts can be found in the [Deephaven Core Community Tutorials](https://deephaven.io/core/docs/tutorials/tutorial/).

## Executing Queries
Queries are executed as in the following code snippet from any JUnit test.
````
api.query(query).fetchAfter("record_count", table->{
	int recCount = table.getSum("RecordCount").intValue();
	assertEquals("Wrong record count", scaleRowCount, recCount);
}).execute();
api.awaitCompletion();
````
Before execution, every property like "${kafka.consumer.addr}" will be replaced with a corresponding values from one of the following (in order):
- Profile Properties: Properties loaded from properties passed into the JVM with *-Dbenchmark.profile=my.properties* or, if that is missing, *default.properties* from this project
- System Properties: Properties from *System.getProperty(name)* in the JVM
- Environment Variables: Variables set in the OS environment and retrieved with *System.env(name)*

Tables are created in the engine according to the query script, and results may be retrieved in two ways;
- fetchAfter: Fetches a table snapshot after the query is completed
- fetchDuring: Fetches a table snapshot peridodically according to the ticking rate defined for the engine and client session

For comparison, tables can be viewed in the [Local Deephaven UI](http://localhost:10000/ide) the tests are running against.

## Measuring Results
Given that the tests are built to scale, it doesn't make sense to collect only elapsed time for each test run.  Processing rates are much more informative when processing varying numbers of records.

Available result rates:
- setup: Measures setup of the test
- test: Measures the relevant part of the running test
- teardown: Measures the teardown of the test

Results are measured with the API's timer as in the following example:
````
var tm = api.timer();

api.query(query);
api.awaitCompletion();

api.result().test(tm, scaleRowCount);
````
The timer is initiated before the query is executed, and the result is recorded when it returns.
