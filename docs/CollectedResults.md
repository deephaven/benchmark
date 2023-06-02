# Collected Results

Running Benchmark tests in an IDE produces results in a directory structure in the current (working) directory.  Running the same tests 
from the command line through the deephaven-benchmark uber-jar yields the same directory structure for each run while accumulating the
data from repeated runs instead of overwriting it.

### Example IDE-driven Directory Structure
````
results/
	benchmark-metrics.csv
	benchmark-platform.csv
	benchmark-results.csv
	test-logs/
		io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromKafkaStream.query.md
		io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromParquetAndStream.query.md
````

### Example Command-line-driven Directory Structure
````
results/
	run-17d06a7611
		benchmark-metrics.csv
		benchmark-platform.csv
		benchmark-results.csv
		test-logs/
			io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromKafkaStream.query.md
			io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromParquetAndStream.query.md
	run-17d9ec2f2e
		benchmark-metrics.csv
		benchmark-platform.csv
		benchmark-results.csv
		test-logs/
			io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromKafkaStream.query.md
			io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromParquetAndStream.query.md
````

What does each file mean?
- run\-\<id\>: Time-based unique id for the batch of tests that was run
- benchmark-metrics.csv: MXBean and other metrics collected over the test run
- benchmark-platform.csv: Various VM and hardware details for the components of the test system
- benchmark-results.csv: Query rates for the running tests at scale
- test-logs: Directory containing details about each test run according to test class
- \*.query.md: A log showing the queries that where executed to complete each test in the order they were executed

## Benchmark Platform CSV

The benchmark-platform csv contains details for the system/VM where the test runner and Deephave Engine ran. 
Details include Available Processors, JVM version, OS version, etc.

Columns defined in the file are:
- origin: The service where the property was collected
- name: The property name
- value: The property value for the service

Properties defined in the file are:
- java.version: The version of java running the application
- java.vm.name: The name of the java virtual machine running the application
- java.class.version: The class version the java VM supports
- os.name: The name of the operating system hosting the application
- os.version: The version of the operating system hosting the application
- available.processors: The number of CPUs the application is allowed to use
- java.max.memory: Maximum amount of memory the application is allowed to use 
- python.version: The version of python used in the Deephaven Engine
- deephaven.version: The version of Deephaven tested against (client and server may be different)


### Example benchmark-platform.csv
````
origin,              name,                    value
test-runner,         java.version,            17.0.1
test-runner,         java.vm.name,            OpenJDK 64-Bit Server VM
test-runner,         java.class.version,      61
test-runner,         os.name,                 Windows 10
test-runner,         os.version,              10
test-runner,         available.processors,    16
test-runner,         java.max.memory,         15.98G
test-runner,         deephaven.version,       0.22.0
deephaven-engine,    java.version,            17.0.5
deephaven-engine,    java.vm.name,            OpenJDK 64-Bit Server VM
deephaven-engine,    java.class.version,      61
deephaven-engine,    os.name,                 Linux
deephaven-engine,    os.version,              5.15.79.1-microsoft-standard-WSL2
deephaven-engine,    available.processors,    12
deephaven-engine,    java.max.memory,         42.00G
deephaven-engine,    python.version,          3.10.6
````

## Benchmark Results CSV

The benchmark-results.csv contains measurements taken of the course of each test run. One row is listed for each test.

Fields supplied in the file are:
- benchmark_name: The unique name of the benchmark
- origin: The serice where the benchmark was collected
- timestamp: Millis since epoch at the beginning of the benchmark
- test_duration: Seconds elapsed for the entire test run including setup and teardown
- op_duration: Seconds elapsed for the operation under measurement
- op_rate: Processing rate supplied by the test-writer
- row_count: The number of rows processed by the operation

### Example benchmark-results.csv
````
benchmark_name,origin,timestamp,test_duration,op_duration,op_rate,row_count
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926395799,155.4960,5.4450,367309458,2000000000
Select- 1 Calc Using 2 Cols -Inc,deephaven-engine,1683926551491,12.1970,3.5900,111420612,400000000
Select- 2 Cals Using 2 Cols -Static,n/a,1683926563714,8.7480,8.7480,1143118,10000000
SelectDistinct- 1 Group 250 Unique Vals -Static,deephaven-engine,1683926572487,195.0420,18.5530,64679566,1200000000
````

## Benchmark Metrics CSV

The benchmark-metrics.csv contains metrics collected while running the benchmark.  Most metrics (like MXBean metrics) represent a snapshot
at a moment in time. When these snapshots are taken and collected are up to the test-writer.  For example, in the standard benchmarks
available in this project, metrics snaphosts are taken before and after the benchmark operation. The before-after metrics can be compared
to calculate things like Heap Gain or garbage collection counts.

Field supplied in the file are:
- benchmark_name: The unique name of the benchmark
- origin: The serice where the metric was collected
- timestamp: Millis since epoch when the metrics was recorded
- category: A grouping category for the metric
- type: The type of metric has been collected (should be more narrowly focused than category)
- name: A metric name that is unique within the category
- value: The numeric value of the metric
- note: Any addition clarifying information

## Example benchmark-metrics.csv
````
benchmark_name,origin,timestamp,category,type,name,value,note
Select- 1 Calc Using 2 Cols -Static,test-runner,1683926402500,setup,docker,restart,6700,standard
Select- 1 Calc Using 2 Cols -Static,test-runner,1683926479998,source,generator,duration.secs,74.509,
Select- 1 Calc Using 2 Cols -Static,test-runner,1683926479998,source,generator,record.count,50000000,
Select- 1 Calc Using 2 Cols -Static,test-runner,1683926479998,source,generator,send.rate,671059.8719617764,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,ClassLoadingImpl,ClassLoading,TotalLoadedClassCount,13172,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,ClassLoadingImpl,ClassLoading,UnloadedClassCount,16,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,ObjectPendingFinalizationCount,0,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,HeapMemoryUsage Committed,9126805504,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,HeapMemoryUsage Init,1157627904,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,HeapMemoryUsage Max,25769803776,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,HeapMemoryUsage Used,2963455008,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,NonHeapMemoryUsage Committed,106430464,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,NonHeapMemoryUsage Init,7667712,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,NonHeapMemoryUsage Max,-1,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,MemoryImpl,Memory,NonHeapMemoryUsage Used,101707208,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,HotSpotThreadImpl,Threading,ThreadCount,50,
Select- 1 Calc Using 2 Cols -Static,deephaven-engine,1683926545385,HotSpotThreadImpl,Threading,PeakThreadCount,50,
````

## Query Log

Query logs record queries in the order in which they were run during a test. These include queries run by the framework automatically behind the scenes. 
Any property variables supplied in a query are replaced with the actual values used during the test run. After a test is run, it is usually possible to 
copy and paste the query into the Deephaven UI and run it, because parquet and kafka topic data is left intact. However, if Test B is run after Test A, 
running the recorded queries for Test A may not work, since cleanup is done automatically between tests.
The log is in Markdown format for easier viewing.

### Example Query Log
~~~~
# Test Class - io.deephaven.benchmark.tests.internal.examples.stream.JoinTablesFromKafkaStreamTest

## Test - Count Records From Kakfa Stream

### Query 1
````
from deephaven import kafka_consumer as kc
from deephaven.stream.kafka.consumer import TableType, KeyValueSpec

def bench_api_kafka_consume(topic: str, table_type: str):
    t_type = None
    if table_type == 'append': t_type = TableType.append()
    elif table_type == 'blink': t_type = TableType.blink()
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
kafka_stock_trans = bench_api_kafka_consume('stock_trans', 'append')
bench_api_await_table_size(kafka_stock_trans, 100000)
````

### Query 3
````
kafka_stock_trans=None
from deephaven import garbage_collect; garbage_collect()
````
~~~~

The above log is an example of what you would see in *test&#x2011;logs/io.deephaven.benchmark.tests.internal.examples.stream.JoinTablesFromKafkaStreamTest.query.md*
when running the *JoinTablesFromKafkaStreamTest.countRecordsFromKafkaStream()* integration test.

The log has several components:
- Test Class: The fully-qualified class name of the test
- Test: The description of the test the test-writer supplied
- Query 1,2,3: The Deephaven queries executed in Deephaven in the order they were executed

What the queries are doing:
- Query 1: The definitions of some the Bench API convenience functions used in Query 2. The test-writer used *bench_api_kafka_consume()* and
*bench_api_await_table_size()* in the query, and the Bench API automatically added the corresponding function definitions
- Query 2: The part that the test-writer wrote for the test (see *JoinTablesFromKafkaStreamTest.countRecordsFromKafkaStream()*)
- Query 3: The cleanup query added automatically by the Bench API

