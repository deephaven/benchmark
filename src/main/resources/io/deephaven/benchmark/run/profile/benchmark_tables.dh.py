# Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending 
#
# Deephaven query to run against historical benchmark data stored in GCloud bucket and produce
# some useful correlated tables
# Requirements: Deephaven 0.23.0 or greater

import os, re, glob
import deephaven.dtypes as dht
from deephaven import read_csv, merge, agg, empty_table
from urllib.request import urlopen

# Schema for benchmstk-results.csv
s_results = {'benchmark_name':dht.string, 'origin':dht.string,'timestamp':dht.long,'test_duration':dht.double,
    'op_duration':dht.double,'op_rate':dht.long,'row_count':dht.long}

# Get the latest GCloud run_ids for the benchmark category up to max_runs
def get_remote_run_ids(parent_uri, category, max_runs=10):
    run_ids = []
    search_uri = parent_uri + '?delimiter=/&prefix=' + category + '/' + '&max-keys=100000'
    with urlopen(search_uri) as r:
        text = r.read().decode()
        for run_id in re.findall('<Prefix>{}/run-([a-z0-9]+)/</Prefix>'.format(category), text, re.MULTILINE):
            run_ids.append(run_id)
    run_ids.sort(reverse=True)
    return run_ids[:max_runs]
    
# Get the latest file-based run_ids for the benchmark category up to max_runs
def get_local_run_ids(parent_uri, category, max_runs=10):
    run_ids = []
    text = '\n'.join(glob.glob(parent_uri.replace('file://','') + '/' + category + '/run-*'))
    for run_id in re.findall('.*/run-([a-z0-9]+)'.format(category), text, re.MULTILINE):
        run_ids.append(run_id)
    run_ids.sort(reverse=True)
    return run_ids[:max_runs]
    
def get_run_ids(storage_uri, category, max_runs):
    if storage_uri.startswith('http'):
        return get_remote_run_ids(storage_uri, category, max_runs)
    else: 
        return get_local_run_ids(storage_uri, category, max_runs)

# Merge together benchmark runs from the GCloud bucket for the same csv (e.g. benchmark_results.csv)
def merge_run_tables(parent_uri, run_ids, category, csv_file_name, schema = None):
    merged_table = None
    for run_id in run_ids:
        table_uri = parent_uri + '/' + category + '/run-' + run_id + '/' + csv_file_name
        print("Getting " + table_uri)
        table_csv = read_csv(table_uri, schema) if schema else read_csv(table_uri)
        table_csv = table_csv.update(['run_id = "' + run_id + '"'])
        merged_table = merge([merged_table, table_csv]) if merged_table else table_csv
    return merged_table

# Load standard tables from GCloud or local storage according to category
# If this script is run from exec(), accept the benchmark_category_arg
default_storage_uri = 'https://storage.googleapis.com/deephaven-benchmark'
default_category = 'nightly'
default_max_runs = 5
default_history_runs = 5

storage_uri = benchmark_storage_uri_arg if 'benchmark_storage_uri_arg' in globals() else default_storage_uri
category = benchmark_category_arg if 'benchmark_category_arg' in globals() else default_category
max_runs = benchmark_max_runs_arg if 'benchmark_max_runs_arg' in globals() else default_max_runs
history_runs = benchmark_history_runs_arg if 'benchmark_history_runs_arg' in globals() else default_history_runs
run_ids = get_run_ids(storage_uri, category, max_runs)
bench_results = merge_run_tables(storage_uri, run_ids, category, 'benchmark-results.csv', s_results)
bench_metrics = merge_run_tables(storage_uri, run_ids, category, 'benchmark-metrics.csv')
bench_platforms = merge_run_tables(storage_uri, run_ids, category, 'benchmark-platform.csv')

# Make diff between first and last metrics samples
bench_metrics_diff = bench_metrics.agg_by(
    aggs=[agg.first('first_value=value'), agg.last('last_value=value')], 
    by=['run_id', 'benchmark_name', 'origin', 'category', 'type', 'name'])
bench_metrics_diff = bench_metrics_diff.view(
    ['run_id', 'benchmark_name', 'origin', 'category', 'type', 'name', 'value_diff=last_value-first_value']
)

# Create a table with useful diff metrics joined into the benchmark results
bench_results_diff = bench_results
def add_metric_value_diff(mcategory, mtype, mname, new_mname):
    global bench_results_diff
    single_metrics = bench_metrics_diff.where(['category=mcategory', 'type=mtype', 'name=mname'])
    bench_results_diff = bench_results_diff.exact_join(
        single_metrics, on=['run_id', 'benchmark_name', 'origin'], joins=[new_mname+'=value_diff']
    )

def add_platform_value(pname, new_pname):
    global bench_results_diff
    single_platforms = bench_platforms.where(['name=pname'])
    bench_results_diff = bench_results_diff.exact_join(
        single_platforms, on=['run_id', 'origin'], joins=[new_pname+'=value']
    )

add_platform_value('deephaven.version', 'deephaven_version')
add_metric_value_diff('MemoryImpl', 'Memory', 'HeapMemoryUsage Used', "heap_used")
add_metric_value_diff('MemoryImpl', 'Memory', 'NonHeapMemoryUsage Used', "non_heap_used")
add_metric_value_diff('MemoryImpl', 'Memory', 'HeapMemoryUsage Committed', 'heap_committed')
add_metric_value_diff('MemoryImpl', 'Memory', 'NonHeapMemoryUsage Committed', 'non_heap_committed')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Young Generation', 'CollectionCount', 'g1_young_collect_count')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Young Generation', 'CollectionTime', 'g1_young_collect_time')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Old Generation', 'CollectionCount', 'g1_old_collect_count')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Old Generation', 'CollectionTime', 'g1_old_collect_time')

# Create a table showing percentage variability and change in rates
import statistics
def rstd(rates):
    return statistics.pstdev(rates) * 100.0 / statistics.mean(rates)

from array import array
def rchange(rates) -> float:
    rates = array('l', rates)
    if(len(rates) < 2): return 0.0
    m = statistics.mean(rates[:-1])
    return (rates[-1] - m) / m * 100.0

def format_rates(rates):
    return ' '.join("{:,}".format(r) for r in rates)
    
def gain(start:float, end:float) -> float:
    return (end - start) / start * 100.0

from deephaven.updateby import rolling_group_tick
op_group = rolling_group_tick(cols=["op_group_rates = op_rate"], rev_ticks=history_runs, fwd_ticks=0)
op_version = rolling_group_tick(cols=["op_group_versions = deephaven_version"], rev_ticks=history_runs, fwd_ticks=0)

bench_results_change = bench_results_diff.sort(['benchmark_name', 'origin', 'deephaven_version', 'timestamp'])
bench_results_change = bench_results_change.update_by(ops=[op_group, op_version], by=['benchmark_name', 'origin'])
bench_results_change = bench_results_change.update(
    ['op_rate_variability=(float)rstd(op_group_rates)', 'op_rate_change=(float)rchange(op_group_rates)']
)
bench_results_change = bench_results_change.view(
    ['benchmark_name', 'origin', 'timestamp', 'deephaven_version', 'op_duration', 'op_rate', 
    'op_rate_variability', 'op_rate_change', 'op_rate_change', 'op_group_rates', 'op_group_versions']
)
