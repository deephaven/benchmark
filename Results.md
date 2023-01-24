# Benchmark - Results

Running Benchmark tests in the IDE produces results into a directory structure in the current (working) directory.

````
results/
	benchmark-platform.csv
	benchmark-results.csv
	test-logs/
		io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromKafkaStream.query.md
		io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromParquetAndStream.query.md
````

What does each file mean?
- benchmark-platform.csv: Various VM and hardware details for the components of the test system
- benchmark-results.csv: Query rates for the running tests at scale
- test-logs: Directory containing details about each test run according to test class
- \*.query.md: A log showing the queries that where executed to complete each test in the order they were executed



## Benchmark Platform CSV

The benchmark-platform csv contains details for the system/VM the test runner and Deephave Engine are running on including Available Processors, JVM version, OS version, etc.

Properties defined in the file are:
- java.version: The version of java running the application
- java.vm.name: The name of the java virtual machine running the application
- java.class.version: The class version the java VM supports
- os.name: The name of the operating system hosting the application
- os.version: The version of the operating system hosting the application
- available.processors: The number of CPUs the application is allowed to use
- java.max.memory: Maximum Gigabytes of memory the application is allowed to use 

### Example benchmark-platform.csv
````
application,         name,                    value
test-runner,         java.version,            17.0.1
test-runner,         java.vm.name,            OpenJDK 64-Bit Server VM
test-runner,         java.class.version,      61
test-runner,         os.name,                 Windows 10
test-runner,         os.version,              10
test-runner,         available.processors,    16
test-runner,         java.max.memory,         15.98G
deephaven-engine,    java.version,            17.0.5
deephaven-engine,    java.vm.name,            OpenJDK 64-Bit Server VM
deephaven-engine,    java.class.version,      61
deephaven-engine,    os.name,                 Linux
deephaven-engine,    os.version,              5.15.79.1-microsoft-standard-WSL2
deephaven-engine,    available.processors,    12
deephaven-engine,    java.max.memory,         42.00G
````

## Benchmark Results CSV

The benchmark-results.csv contains measurements taken of the course of each test run. One row is listed for each test.

Fields supplied in the file are:
- name: The name of the test
- timestamp: Millis since epoch at the beginning of the test
- duration: Seconds elapsed for the entire test run including setup and teardown
- test-rate: The user-supplied processing rate in seconds for the test (i.e. rows/sec)
- test-row-count: The number of rows processed

### Example benchmark-results.csv
````
name,timestamp,duration,test-rate,test-row-count
Join Two Tables Using Kakfa Streams - Longhand Query,1672357348025,6.63,44208.66,100000
Join Two Tables Using Kakfa Streams - Shorthand Query,1672357354657,7.96,25125.62,100000
Join Two Tables Using Parquet File Views,1672357362620,6.57,523560.22,100000
Join Two Tables Using Incremental Release of Paquet File Records,1672357369192,6.13,91575.09,100000
````

## Query Log

Query logs record queries in the order in which they were run during a test. These include queries run by the framework automatically behind the scenes. 
Any property variables supplied in a query are replaced with the actual values used during the test run. After a test is run, it is usually possible to 
copy and paste the query into the Deephaven UI and run it, because parquet and kafka topic data is left intact. However, if Test B is run after Test A, 
cut and paste of the queries recorded for Test A may not work. 
The log is in Markdown format for easier viewing.

### Example Query Log
~~~~
# Test Class - io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromKafkaStream

## Test - Count Records From Kakfa Stream

### Query 1
````
from deephaven import kafka_consumer as kc
from deephaven.stream.kafka.consumer import TableType, KeyValueSpec

def bench_api_kafka_consume(topic: str, table_type: str):
	t_type = None
	if table_type == 'append': t_type = TableType.append()
	elif table_type == 'stream': t_type = TableType.stream()
	elif table_type == 'ring': t_type = TableType.ring()
	else: raise Exception('Unsupported kafka stream type: {}'.format(t_type))

	return kc.consume(
		{ 'bootstrap.servers' : 'redpanda:29092', 'schema.registry.url' : 'http://redpanda:8081' },
		topic, partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
		key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec(topic + '_record', schema_version='1'),
		table_type=t_type)

from deephaven.table import Table
from deephaven.ugp import exclusive_lock

def bench_api_await_table_size(table: Table, row_count: int):
	with exclusive_lock():
		while table.j_table.size() < row_count:
			table.j_table.awaitUpdate()

````

### Query 2
````
from deephaven import agg

kafka_stock_trans = bench_api_kafka_consume('stock_trans', 'append')
bench_api_await_table_size(kafka_stock_trans, 100000)

````
~~~~

In the above example, the user made a simple test to load a fixed number of records into a table from a kafka consumer (Query2). 
However, since "bench_api_" functions where used, the definitions of those functions were automatically published to Deephaven Engine ahead of the test run.

