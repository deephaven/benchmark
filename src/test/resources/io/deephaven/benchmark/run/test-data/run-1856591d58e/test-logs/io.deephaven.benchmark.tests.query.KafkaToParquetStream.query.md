# Test Class - io.deephaven.benchmark.tests.query.KafkaToParquetStream

## Test - KafkaToParquetStream

### Query 1
````
table_parquet = '/data/orders.parquet'
table_gen_parquet = '/data/benchmark.FgaGedc.1.G-F9HA.gen.parquet'
table_gen_def_text = '''row.count=100000
compression=ZSTD
isfixed=false
name,type,values
symbol,string,SYM[1-1000]
price,float,[10-20]
qty,int,1
'''
table_gen_def_file = '/data/benchmark.FgaGedc.1.G-F9HA.gen.def'

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
from deephaven.parquet import read
from deephaven import agg

orders = read("/data/orders.parquet")
result = orders.view(formulas=["qty"]).agg_by([agg.sum_("RecCount = qty")], "qty")
````

