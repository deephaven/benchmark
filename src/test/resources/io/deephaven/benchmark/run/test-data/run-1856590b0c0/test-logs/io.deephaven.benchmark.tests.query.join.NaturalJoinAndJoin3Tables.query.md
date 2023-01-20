# Test Class - io.deephaven.benchmark.tests.query.join.NaturalJoinAndJoin3Tables

## Test - Join animals and adjectives - Parquet Views

### Query 1
````
table_parquet = '/data/animals.parquet'
table_gen_parquet = '/data/benchmark.FgaE_Wg.1.IZzEEw.gen.parquet'
table_gen_def_text = '''row.count=250
compression=ZSTD
isfixed=true
name,type,values
animal_id,int,[1-250]
animal_name,string,animal[1-250]
'''
table_gen_def_file = '/data/benchmark.FgaE_Wg.1.IZzEEw.gen.def'

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
table_parquet = '/data/adjectives.parquet'
table_gen_parquet = '/data/benchmark.FgaFBdw.1.qmX8mA.gen.parquet'
table_gen_def_text = '''row.count=644
compression=ZSTD
isfixed=true
name,type,values
adjective_id,int,[1-644]
adjective_name,string,[1-644]
'''
table_gen_def_file = '/data/benchmark.FgaFBdw.1.qmX8mA.gen.def'

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
table_parquet = '/data/relation.parquet'
table_gen_parquet = '/data/benchmark.FgaFBj4.1.hgZ6yA.gen.parquet'
table_gen_def_text = '''row.count=100000
compression=ZSTD
isfixed=true
name,type,values
Values,int,[1-100000]
adjective_id,int,[1-644]
animal_id,int,[1-250]
'''
table_gen_def_file = '/data/benchmark.FgaFBj4.1.hgZ6yA.gen.def'

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

### Query 4
````
from deephaven.parquet import read

animals = read('/data/animals.parquet')
adjectives = read('/data/adjectives.parquet')
relation = read('/data/relation.parquet')

result = relation.natural_join(adjectives, on=['adjective_id']).join(animals, on=['animal_id']).view(formulas=['Values', 'adjective_name', 'animal_name'])

from deephaven.column import int_col
result_row_count = new_table([int_col("result_size", [result.size])])
````

## Test - Join animals and adjectives - Incremental Release

### Query 1
````
table_parquet = '/data/animals.parquet'
table_gen_parquet = '/data/benchmark.FgaFCeY.1.56LMnA.gen.parquet'
table_gen_def_text = '''row.count=250
compression=ZSTD
isfixed=true
name,type,values
animal_id,int,[1-250]
animal_name,string,animal[1-250]
'''
table_gen_def_file = '/data/benchmark.FgaFCeY.1.56LMnA.gen.def'

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
table_parquet = '/data/adjectives.parquet'
table_gen_parquet = '/data/benchmark.FgaFCgY.1.3yci-Q.gen.parquet'
table_gen_def_text = '''row.count=644
compression=ZSTD
isfixed=true
name,type,values
adjective_id,int,[1-644]
adjective_name,string,[1-644]
'''
table_gen_def_file = '/data/benchmark.FgaFCgY.1.3yci-Q.gen.def'

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
table_parquet = '/data/relation.parquet'
table_gen_parquet = '/data/benchmark.FgaFCkM.1.mnJWKw.gen.parquet'
table_gen_def_text = '''row.count=100000
compression=ZSTD
isfixed=true
name,type,values
Values,int,[1-100000]
adjective_id,int,[1-644]
animal_id,int,[1-250]
'''
table_gen_def_file = '/data/benchmark.FgaFCkM.1.mnJWKw.gen.def'

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

### Query 4
````
from deephaven.parquet import read

animals = read('/data/animals.parquet')
adjectives = read('/data/adjectives.parquet')
relation = read('/data/relation.parquet').select(formulas=['adjective_id', 'animal_id', 'Values'])

autotune = jpy.get_type('io.deephaven.engine.table.impl.select.AutoTuningIncrementalReleaseFilter')
relation_filter = autotune(0, 1000000, 1.0, True)
relation = relation.where(relation_filter)

result = relation.natural_join(adjectives, on=['adjective_id']).join(animals, on=['animal_id']).view(formulas=['Values', 'adjective_name', 'animal_name'])

relation_filter.start()
relation_filter.waitForCompletion()

from deephaven.column import int_col
result_row_count = new_table([int_col("result_size", [result.size])])
````

