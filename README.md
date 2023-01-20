# Benchmark #

Benchmark is a framework designed to work from the command line and through popular Java IDEs using the JUnit framework as a runner.  It is geared towards scale testing interfaces capable of ingesting table data, transforming it, and returning tabular results.  It represents a follow-on to the [Bencher Project](https://github.com/deephaven/bencher) that benchmarks many of the query features of [Deephaven Core Community](https://deephaven.io/community/).

For the present, tests are geared towards testing [Deephaven Core Community](https://deephaven.io/community/) through the Barrage Java Client.  Tests focus on querying static parquet files, streamed Kafka topics, and replayed data.

The typical workflow of a Benchmark test is... *Configure table/column generation* --> *Execute Query* --> *Measure Results*.  This is all done inside a Junit test class.

Tests are designed to scale by changing a scale property value call *scale.row.count*, so the same test can be used in multiple runs at different scales for comparison.  For ease of comparison, collected results are processing rates rather than elapsed time.

Results for a test run are output to the console and stored in the current directory in *benchmark-results.csv*.

More Resources:
- [Benchmark - Bencher Comparison](VersusBencher.md)
- [Benchmark - Command Line](CommandLine.md)
- [Benchmark - Results](Results.md)

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

## Some Test Results

### Test setup:
- Windows 11 with 64G RAM and 16 CPU threads
- WSL 2 limited to 44G RAM and 12 CPU threads
- Bencher runs both tests and Engine/Redpanda in WSL
- Benchmark runs test on Windows and Engine/Redpanda in WSL
- Benchmark uses ZSTD compression for Kafka producer to broker
- Bencher uses GZIP and Benchmark uses ZSTD compression for writing parquet files
- Test sourcees are in *src/it/java*

### Bencher vs Benchmark for the same queries (rates are records/sec):
Test Details
- All tests run the same query that joins a table of 10 or 100 million records to a table of 100K records
- Test Description: Rows are either released incrementally (auto increment) or read from a static parquet file (parquet static)
- Bencher Rate: Rows processed per second while running the query in Bencher
- Benchmark Rate: Rows processed per second while running the query in Benchmark

|Test Description|Bencher Rate|Benchmark Rate|
|----------------|------------|-----------|
|stock join 10m auto increment|339430.53|363266.50|
|stock join 10m parquet static|579667.06|569670.70|
|stock join 100m auto increment|358337.90|398665.25|
|stock join 100m parquet static|553502.34|693847.00|

### Benchmark producing records to the Kafka broker and consumed by the Deephaven query script (rates are records/sec):
Test Details
- All tests produce data to Kafka while it is consumed/queried in Deephaven
- First two tests run the same query that joins a table of 10 or 100 million records to a table of 100K records
- Last test loads a table of 100 million rows from Kafka
- Test Description: Test data is produced to Kafka and consumed in the query using Avro spec
- Query Rate: Rows processed per second while running the query in Benchmark
- Producer Rate: Rows produce to Kafka per seconds. 
- If Query Rate is significantly lower than Producer Rate, Deephaven Engine isn't keeping up

|Test Description|Query Rate|Producer Rate|
|----------------|-----------|-------------|
|stock join 10m kafka append|224411.48|415973.38|
|stock join 100m kafka append|275232.62|384831.48|
|consumer count 100m kafka append|441027.97|442944.34|



