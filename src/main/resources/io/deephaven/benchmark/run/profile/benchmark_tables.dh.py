# Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending 
#
# Deephaven query to run against historical benchmark data stored in GCloud bucket and produce
# some useful correlated tables
# Requirements: Deephaven 0.23.0 or greater

import os, re, glob, jpy
import deephaven.dtypes as dht
from deephaven import read_csv, merge, agg, empty_table
from urllib.request import urlopen, urlretrieve
from numpy import typing as npt

# Schema for benchmstk-results.csv
s_results = {'benchmark_name':dht.string, 'origin':dht.string,'timestamp':dht.long,'test_duration':dht.double,
    'op_duration':dht.double,'op_rate':dht.long,'row_count':dht.long}

# Convert the given name to a name suitable for a DH column name
def normalize_name(name):
    name = name.replace('/','__')
    return re.sub('[^A-Za-z0-9_$]', '_', name)

# Get the latest GCloud run_ids for the benchmark category up to max_runs
def get_remote_children(parent_uri, category, max_runs=10):
    run_ids = []
    search_uri = parent_uri + '?delimiter=/&prefix=' + category + '/' + '&max-keys=100000'
    with urlopen(search_uri) as r:
        text = r.read().decode()
        for run_id in re.findall('<Prefix>{}/([^/><]+)/</Prefix>'.format(category), text, re.MULTILINE):
            run_ids.append(run_id)
    run_ids.sort(reverse=True)
    return run_ids[:max_runs]
    
# Get the file-based children of the given parent/category directory
def get_local_children(parent_uri, category, max_runs=10):
    run_ids = []
    root_path = parent_uri.replace('file:///','/')
    for run_id in os.listdir(os.path.normpath(os.path.join(root_path, category))):
        run_ids.append(run_id)
    run_ids.sort(reverse=True)
    return run_ids[:max_runs]
    
# Get the children of the given storage/category uri
def get_children(storage_uri, category, max_runs):
    if storage_uri.startswith('http'):
        return get_remote_children(storage_uri, category, max_runs)
    else: 
        return get_local_children(storage_uri, category, max_runs)
        
def get_run_paths(storage_uri, category, actor_filter, set_filter, max_set=10):
    print('Get Run Paths', storage_uri, category, actor_filter, set_filter, max_set)
    set_matcher = re.compile(set_filter)
    actor_matcher = re.compile(actor_filter)
    benchmark_sets = []
    for actor in get_children(storage_uri, category, 1000):
        if actor_matcher.match(actor): 
            for set_label in get_children(storage_uri, category + '/' + actor, 1000):
                if set_matcher.match(set_label): 
                    benchmark_sets.append(actor + '/' + set_label)
    benchmark_sets.sort(reverse=True)
    benchmark_sets = benchmark_sets[:max_sets]
    benchmark_runs = []
    for set_path in benchmark_sets:
        for run_id in get_children(storage_uri, category + '/' + set_path, 1000):
            benchmark_runs.append(set_path + '/' + run_id)
    return benchmark_runs

# Cache an HTTP url into a local directory and return the local path  
def cache_remote_csv(uri):
    try:
        out_path = re.sub('^http.*/deephaven-benchmark/', '/data/deephaven-benchmark/', uri)
        os.makedirs(os.path.dirname(out_path), mode=0o777, exist_ok=True)
    except Exception as ex:
        print('Error downloading file:', out_path, ':', ex)
        return uri
    try:
        out_path_gz = out_path + '.gz'
        if os.path.exists(out_path_gz): return out_path_gz
        urlretrieve(uri + '.gz', out_path_gz)
        print('Cache', uri + '.gz')
        return out_path_gz
    except Exception:
        try:
            if os.path.exists(out_path): return out_path
            urlretrieve(uri, out_path)
            print('Cache', uri)
            return out_path
        except Exception as ex:
            print('Error caching file:', out_path, ':', ex)
            return uri

# Read csv into a table (Currently, pandas is used for gzipped csv)
def dh_read_csv(uri, schema=None):
    uri = uri.replace('file:///','/')
    uri = cache_remote_csv(uri) if uri.startswith('http') else uri
    try:
        tbl = read_csv(uri + '.gz', schema) if schema else read_csv(uri + '.gz')
        print('Load ' + uri + '.gz')
    except Exception:
        tbl = read_csv(uri, schema) if schema else read_csv(uri)
        print('Load ' + uri)
    return tbl

# Merge together benchmark runs from the GCloud bucket for the same csv (e.g. benchmark_results.csv)
def merge_run_tables(parent_uri, run_ids, category, csv_file_name, schema = None):
    tables = []
    for run_id in run_ids:
        table_uri = parent_uri + '/' + category + '/' + run_id + '/' + csv_file_name
        table_csv = dh_read_csv(table_uri, schema)
        set_id = os.path.dirname(run_id)
        run_id = os.path.basename(run_id)
        table_csv = table_csv.update_view(['set_id = "' + set_id + '"', 'run_id = "' + run_id + '"'])
        tables.append(table_csv)
    return merge(tables)

# Load standard tables from GCloud or local storage according to category
default_storage_uri = 'https://storage.googleapis.com/deephaven-benchmark'
default_category = 'adhoc'
default_max_sets = 100
default_history_runs = 5
default_actor_filter = '.*'
default_set_filter = '.*'
default_platform_props = []
default_metric_props = []

storage_uri = 'file:///data/deephaven-benchmark'  #benchmark_storage_uri_arg if 'benchmark_storage_uri_arg' in globals() else default_storage_uri
category = benchmark_category_arg if 'benchmark_category_arg' in globals() else default_category
max_sets = benchmark_max_sets_arg if 'benchmark_max_sets_arg' in globals() else default_max_sets
history_runs = benchmark_history_runs_arg if 'benchmark_history_runs_arg' in globals() else default_history_runs
actor_filter = benchmark_actor_filter_arg if 'benchmark_actor_filter_arg' in globals() else default_actor_filter
set_filter = benchmark_set_filter_arg if 'benchmark_set_filter_arg' in globals() else default_set_filter
platform_props = benchmark_platform_props_arg if 'benchmark_platform_props_arg' in globals() else default_platform_props
metric_props = benchmark_metric_props_arg if 'benchmark_metric_props_arg' in globals() else default_metric_props
run_ids = get_run_paths(storage_uri, category, actor_filter, set_filter, max_sets)

bench_results = merge_run_tables(storage_uri, run_ids, category, 'benchmark-results.csv', s_results)
bench_metrics = merge_run_tables(storage_uri, run_ids, category, 'benchmark-metrics.csv')
bench_platforms = merge_run_tables(storage_uri, run_ids, category, 'benchmark-platform.csv')

# Add columns for the specified platform properties
def add_platform_values(table, pnames=[], cnames = []):
    pnames = list(dict.fromkeys(pnames))
    for pname in pnames:
        new_pname = normalize_name(pname)
        cnames.append(new_pname)
        single_platforms = bench_platforms.where(['name=pname']).first_by(['set_id','run_id','origin'])
        table = table.natural_join(
            single_platforms, on=['set_id','run_id','origin'], joins=[new_pname+'=value']
        )
    return table

# Add columns for the specified metric properties
def add_metric_values(table, pnames=[], cnames=[]):
    pnames = list(dict.fromkeys(pnames))
    for pname in pnames:
        new_pname = normalize_name(pname)
        cnames.append(new_pname)
        single_metrtics = bench_metrics.where(['name=pname']).first_by(['benchmark_name','set_id','run_id','origin'])
        table = table.natural_join(
            single_metrtics, on=['benchmark_name','set_id','run_id','origin'], joins=[new_pname+'=value']
        )
    return table

import statistics
def rstd(rates) -> float:
    rates = [i for i in rates if i >= 0]
    mean = statistics.mean(rates)
    return (statistics.pstdev(rates) * 100.0 / mean) if mean != 0 else 0.0
    
def zscore(rate, rates) -> float:
    rates = [i for i in rates if i >= 0]
    std = statistics.pstdev(rates)
    return ((rate - statistics.mean(rates)) / std) if std != 0 else 0.0

def zprob(zscore) -> float:
    lower = -abs(zscore)
    upper = abs(zscore)
    return 1 - (statistics.NormalDist().cdf(upper) - statistics.NormalDist().cdf(lower))

from array import array
def rchange(rates) -> float:
    rates = array('l', rates)
    if(len(rates) < 2): return 0.0
    m = statistics.mean(rates[:-1])
    return (rates[-1] - m) / m * 100.0

def gain(start, end) -> float:
    return (end - start) / start * 100.0

def format_rates(rates):
    return ' '.join("{:,}".format(r) for r in rates)

def truncate(text, size):
    if len(text) < size - 3: return text
    return text[:size-3] + '...'

def mid_item(arr):
    n = len(arr)
    return arr[n // 2]

def last_item(arr):
    return arr[-1]

def merge_arrays(type_str, arrs):
    final_arr = []
    for arr in arrs:
        for i in arr.copyToArray():
            final_arr.append(i)
    return jpy.array(type_str, final_arr)

# Create a table showing percentage variability and change in rates

## Reduce the runs in each set to one row
bench_results_sets = bench_results.sort(['benchmark_name','origin','set_id','op_rate']) \
    .group_by(['benchmark_name','origin','set_id']) \
    .view([
        'benchmark_name','origin','timestamp=(long)mid_item(timestamp)','test_duration=(double)mid_item(test_duration)',
        'set_op_rates=op_rate','op_duration=(double)mid_item(op_duration)','op_rate=(long)mid_item(op_rate)','row_count=(long)mid_item(row_count)',
        'variability=(float)rstd(set_op_rates)','set_id','run_id=(String)mid_item(run_id)','set_count=count(set_op_rates)'
    ])

## Attach columns for specified metrics and platform properties
local_platform_props = []
local_metric_props = []
bench_results_sets = add_platform_values(bench_results_sets, ['deephaven.version'] + platform_props, local_platform_props)
bench_results_sets = add_metric_values(bench_results_sets, metric_props, local_metric_props)

# Create a table showing rate changes between sets
local_platform_props = [p + '=last_item(' + p + ')' for p in local_platform_props]
local_metric_props = [p + '=last_item(' + p + ')' for p in local_metric_props]
bench_results_change = bench_results_sets.sort(['benchmark_name','origin','set_id']) \
    .tail_by(10, ['benchmark_name','origin']) \
    .group_by(['benchmark_name','origin']) \
    .view(['benchmark_name','origin','timestamp=last_item(timestamp)'] + local_platform_props + local_metric_props +
        ['test_duration=last_item(test_duration)','op_duration=last_item(op_duration)','op_rate=last_item(op_rate)',
        'set_op_rates=(long[])merge_arrays("long",set_op_rates)','op_rate_variability=(float)rstd(set_op_rates)',
        'op_rate_change=(float)rchange(set_op_rates)','set_id'
    ])


