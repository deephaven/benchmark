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

## Test - Join Two Tables Using Kakfa Streams - Longhand Query

### Query 1
````
from deephaven import agg
from deephaven import kafka_consumer as kc
from deephaven.stream.kafka.consumer import TableType, KeyValueSpec
from deephaven.table import Table
from deephaven.ugp import exclusive_lock

kafka_stock_info = kc.consume(
	{ 'bootstrap.servers' : 'redpanda:29092', 'schema.registry.url' : 'http://redpanda:8081' },
	'stock_info', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
	key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('stock_info_record', schema_version='1'),
	table_type=TableType.append())

kafka_stock_trans = kc.consume(
	{ 'bootstrap.servers' : 'redpanda:29092', 'schema.registry.url' : 'http://redpanda:8081' },
	'stock_trans', partitions=None, offsets=kc.ALL_PARTITIONS_SEEK_TO_BEGINNING,
	key_spec=KeyValueSpec.IGNORE, value_spec=kc.avro_spec('stock_trans_record', schema_version='1'),
	table_type=TableType.append())

stock_info = kafka_stock_info.view(formulas=['symbol', 'description', 'exchange'])
stock_trans = kafka_stock_trans.view(formulas=['symbol', 'timestamp=KafkaTimestamp', 'price', 'buys', 'sells', 'rec_count=1'])

aggs = [
	agg.avg('AvgPrice=price'), agg.min_('LowPrice=price'), agg.max_('HighPrice=price'),
	agg.sum_('Buys=buys'), agg.sum_('Sells=sells'), agg.sum_('RecordCount=rec_count')
]

by = ['symbol', 'description', 'exchange']

formulas = [
	'Symbol=symbol', 'Description=description', 'Exchange=exchange', 'AvgPrice',
	'LowPrice', 'HighPrice', 'Volume=Buys+Sells', 'RecordCount'
]

stock_volume = stock_trans.join(stock_info, on=['symbol']).agg_by(aggs, by).view(formulas)
stock_exchange = stock_volume.agg_by([agg.sum_('Volume'), agg.sum_('RecordCount')], by=['Exchange'])
record_count = stock_exchange.agg_by([agg.sum_('RecordCount')])

def await_table_size(table: Table, row_count: int):
	with exclusive_lock():
		while table.j_table.size() < row_count:
			table.j_table.awaitUpdate()

await_table_size(kafka_stock_trans, 100000)

````

## Test - Join Two Tables Using Kakfa Streams - Shorthand Query

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

kafka_stock_info = bench_api_kafka_consume('stock_info', 'append')
kafka_stock_trans = bench_api_kafka_consume('stock_trans', 'append')

stock_info = kafka_stock_info.view(formulas=['symbol', 'description', 'exchange'])
stock_trans = kafka_stock_trans.view(formulas=['symbol', 'timestamp=KafkaTimestamp', 'price', 'buys', 'sells', 'rec_count=1'])

aggs = [
	agg.avg('AvgPrice=price'), agg.min_('LowPrice=price'), agg.max_('HighPrice=price'),
	agg.sum_('Buys=buys'), agg.sum_('Sells=sells'), agg.sum_('RecordCount=rec_count')
]

by = ['symbol', 'description', 'exchange']

formulas = [
	'Symbol=symbol', 'Description=description', 'Exchange=exchange', 'AvgPrice',
	'LowPrice', 'HighPrice', 'Volume=Buys+Sells', 'RecordCount'
]

stock_volume = stock_trans.join(stock_info, on=['symbol']).agg_by(aggs, by).view(formulas)
stock_exchange = stock_volume.agg_by([agg.sum_('Volume'), agg.sum_('RecordCount')], by=['Exchange'])
record_count = stock_exchange.agg_by([agg.sum_('RecordCount')])

bench_api_await_table_size(kafka_stock_trans, 100000)

````

