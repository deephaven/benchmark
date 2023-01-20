# Test Class - io.deephaven.benchmark.tests.query.examples.stream.JoinTablesFromParquetAndStream

## Test - Join Two Tables Using Parquet File Views

### Query 1
````
table_parquet = '/data/stock_info.parquet'
table_gen_parquet = '/data/benchmark.FgaFPAo.1.I6KSMw.gen.parquet'
table_gen_def_text = '''row.count=10000
compression=ZSTD
isfixed=true
name,type,values
symbol,string,SYM[1-10000]
description,string,ABC[1-10000] CORP
exchange,string,EXCHANGE[1-10]
'''
table_gen_def_file = '/data/benchmark.FgaFPAo.1.I6KSMw.gen.def'

import os, glob
from deephaven import new_table
from deephaven.column import string_col

def findMatchingGenParquet(gen_def_text):
	for path in glob.glob('/data/benchmark.*.*.*.gen.def'):
		with open(path) as f:
			if f.read() == gen_def_text:
				return os.path.splitext(os.path.splitext(path)[0])[0]
	return None

if os.path.exists(table_parquet):
	os.remove(table_parquet)

usedExisting = False
matching_gen_parquet = findMatchingGenParquet(table_gen_def_text)
if matching_gen_parquet is not None and os.path.exists(str(matching_gen_parquet) + '.gen.parquet'):
	os.link(str(matching_gen_parquet) + '.gen.parquet', table_parquet)
	usedExisting = True

result = new_table([string_col("UsedExistingParquet", [str(usedExisting)])])
````

### Query 2
````
table_parquet = '/data/stock_trans.parquet'
table_gen_parquet = '/data/benchmark.FgaFPCc.1.5Y1wag.gen.parquet'
table_gen_def_text = '''row.count=100000
compression=ZSTD
isfixed=false
name,type,values
symbol,string,SYM[1-10000]
price,float,[100-200]
buys,int,[1-100]
sells,int,[1-100]
'''
table_gen_def_file = '/data/benchmark.FgaFPCc.1.5Y1wag.gen.def'

import os, glob
from deephaven import new_table
from deephaven.column import string_col

def findMatchingGenParquet(gen_def_text):
	for path in glob.glob('/data/benchmark.*.*.*.gen.def'):
		with open(path) as f:
			if f.read() == gen_def_text:
				return os.path.splitext(os.path.splitext(path)[0])[0]
	return None

if os.path.exists(table_parquet):
	os.remove(table_parquet)

usedExisting = False
matching_gen_parquet = findMatchingGenParquet(table_gen_def_text)
if matching_gen_parquet is not None and os.path.exists(str(matching_gen_parquet) + '.gen.parquet'):
	os.link(str(matching_gen_parquet) + '.gen.parquet', table_parquet)
	usedExisting = True

result = new_table([string_col("UsedExistingParquet", [str(usedExisting)])])
````

### Query 3
````
from deephaven.time import now
from deephaven import agg
from deephaven.parquet import read

p_stock_info = read('/data/stock_info.parquet')
p_stock_trans = read('/data/stock_trans.parquet')

stock_info = p_stock_info.view(formulas=['symbol', 'description', 'exchange'])
stock_trans = p_stock_trans.view(formulas=['symbol', 'timestamp=now()', 'price', 'buys', 'sells', 'rec_count=1'])

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
````

## Test - Join Two Tables Using Incremental Release of Parquet File Records

### Query 1
````
table_parquet = '/data/stock_info.parquet'
table_gen_parquet = '/data/benchmark.FgaFPnQ.1.zVf89g.gen.parquet'
table_gen_def_text = '''row.count=10000
compression=ZSTD
isfixed=true
name,type,values
symbol,string,SYM[1-10000]
description,string,ABC[1-10000] CORP
exchange,string,EXCHANGE[1-10]
'''
table_gen_def_file = '/data/benchmark.FgaFPnQ.1.zVf89g.gen.def'

import os, glob
from deephaven import new_table
from deephaven.column import string_col

def findMatchingGenParquet(gen_def_text):
	for path in glob.glob('/data/benchmark.*.*.*.gen.def'):
		with open(path) as f:
			if f.read() == gen_def_text:
				return os.path.splitext(os.path.splitext(path)[0])[0]
	return None

if os.path.exists(table_parquet):
	os.remove(table_parquet)

usedExisting = False
matching_gen_parquet = findMatchingGenParquet(table_gen_def_text)
if matching_gen_parquet is not None and os.path.exists(str(matching_gen_parquet) + '.gen.parquet'):
	os.link(str(matching_gen_parquet) + '.gen.parquet', table_parquet)
	usedExisting = True

result = new_table([string_col("UsedExistingParquet", [str(usedExisting)])])
````

### Query 2
````
table_parquet = '/data/stock_trans.parquet'
table_gen_parquet = '/data/benchmark.FgaFPpE.1._jlp-Q.gen.parquet'
table_gen_def_text = '''row.count=100000
compression=ZSTD
isfixed=false
name,type,values
symbol,string,SYM[1-10000]
price,float,[100-200]
buys,int,[1-100]
sells,int,[1-100]
'''
table_gen_def_file = '/data/benchmark.FgaFPpE.1._jlp-Q.gen.def'

import os, glob
from deephaven import new_table
from deephaven.column import string_col

def findMatchingGenParquet(gen_def_text):
	for path in glob.glob('/data/benchmark.*.*.*.gen.def'):
		with open(path) as f:
			if f.read() == gen_def_text:
				return os.path.splitext(os.path.splitext(path)[0])[0]
	return None

if os.path.exists(table_parquet):
	os.remove(table_parquet)

usedExisting = False
matching_gen_parquet = findMatchingGenParquet(table_gen_def_text)
if matching_gen_parquet is not None and os.path.exists(str(matching_gen_parquet) + '.gen.parquet'):
	os.link(str(matching_gen_parquet) + '.gen.parquet', table_parquet)
	usedExisting = True

result = new_table([string_col("UsedExistingParquet", [str(usedExisting)])])
````

### Query 3
````
from deephaven.time import now
from deephaven import agg
from deephaven.parquet import read

p_stock_info = read('/data/stock_info.parquet')

p_stock_trans = read('/data/stock_trans.parquet').select(formulas=['symbol','price','buys','sells'])
autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
relation_filter = autotune(0, 1000000, 1.0, True)

stock_info = p_stock_info.view(formulas=['symbol', 'description', 'exchange'])
stock_trans = p_stock_trans.where(relation_filter).view(formulas=['symbol', 'timestamp=now()', 'price', 'buys', 'sells', 'rec_count=1'])

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

relation_filter.start()
relation_filter.waitForCompletion()

````

