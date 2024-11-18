# Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending 
#
# Deephaven python functions to support Benchmark Dashboards. These functions produce basic tables, 
# format strings, and do calculations. The data for creating tables is downloaded and cached from 
# either the Deephaven Benchmark GCloud bucket or from NFS on one of Deephaven's demos servers.
#
# Requirements: Deephaven 0.36.1 or greater

import os, re, glob, jpy
import deephaven.dtypes as dht
from deephaven import read_csv, merge, agg, empty_table, input_table, dtypes as dht
from urllib.request import urlopen, urlretrieve
from numpy import typing as npt

# Convert the given name to a name suitable for a DH column name
def normalize_name(name):
    name = name.replace('/','__')
    return re.sub('[^A-Za-z0-9_$]', '_', name)

# Get the latest GCloud run_ids for the benchmark category up to max_runs
def get_remote_children(parent_uri, category, max_runs=10):
    run_ids = []
    search_uri = parent_uri + '?delimiter=/&prefix=' + category + '/' + '&max-keys=10000'
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

# Get the paths for benchmark run data that match the given filters
def get_run_paths(storage_uri, category, actor_filter, set_filter, max_sets):
    actor_filter = actor_filter if actor_filter else get_default_actor_filter(category)
    set_filter = set_filter if set_filter else get_default_set_filter(category)
    set_matcher = re.compile(set_filter)
    actor_matcher = re.compile(actor_filter)
    run_matcher = re.compile('run-[0-9A-Za-z]+')
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
            if run_matcher.match(run_id): 
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
def dh_read_csv(uri, convert_func):
    uri = uri.replace('file:///','/')
    uri = cache_remote_csv(uri) if uri.startswith('http') else uri
    try:
        tbl = read_csv(uri + '.gz')
        tbl = convert_func(tbl)
        print('Load ' + uri + '.gz')
    except Exception:
        tbl = read_csv(uri)
        tbl = convert_func(tbl)
        print('Load ' + uri)
    return tbl

# Merge together benchmark runs from the GCloud bucket for the same csv (e.g. benchmark_results.csv)
def merge_run_tables(parent_uri, run_ids, category, csv_file_name, convert_func):
    tables = []
    for run_id in run_ids:
        table_uri = parent_uri + '/' + category + '/' + run_id + '/' + csv_file_name
        table_csv = dh_read_csv(table_uri, convert_func)
        set_id = os.path.dirname(run_id)
        run_id = os.path.basename(run_id)
        table_csv = table_csv.update_view(['set_id = "' + set_id + '"', 'run_id = "' + run_id + '"'])
        tables.append(table_csv)
    return merge(tables)

# Do any conversions of type or column name needed from benchmark-results.csv
def convert_result(table):
    return table.view(['benchmark_name','origin','timestamp=(long)timestamp','test_duration=(double)test_duration',
        'op_duration=(double)op_duration','op_rate=(long)op_rate','row_count=(long)row_count'])
        
# Do any conversions of type or column name needed from benchmark-metrics.csv
def convert_metric(table):
    return table.view(['benchmark_name','origin','timestamp=(long)timestamp','name',
        'value=(double)value','note'])
        
# Do any conversions of type or column name needed from benchmark-platform.csv
def convert_platform(table):
    return table.view(['origin','name','value'])

# Get the default actor filter depending on the given category
def get_default_actor_filter(category):
    if category in ['release','nightly','compare']: return 'deephaven'
    return '.+'

# Get the default set filter depending on the given category
def get_default_set_filter(category):
    if category in ['release','compare']: return '[0-9]{2}[.][0-9]{3}[.][0-9]{2}'  # ##.###.##
    if category in ['nightly']: return '[0-9]{4}([-][0-9]{2}){2}'  # yyyy-mm-dd
    return '.+'
    
def empty_bench_results():
    return input_table({'benchmark_name':dht.string,'origin':dht.string,'timestamp':dht.int64,
        'test_duration':dht.float64,'op_duration':dht.float64,'op_rate':dht.int64,
        'row_count':dht.int64,'set_id':dht.string,'run_id':dht.string})

def empty_bench_result_sets():
    sets = input_table({'benchmark_name':dht.string,'origin':dht.string,'timestamp':dht.int64,
        'test_duration':dht.float64,'set_op_rates':dht.int64_array,'op_duration':dht.float64,
        'op_rate':dht.int64,'row_count':dht.int64,'variability':dht.float32,'set_id':dht.string,
        'run_id':dht.string,'set_count':dht.int64,'deephaven_version':dht.string})
    return sets, empty_bench_results()

def empty_bench_platform():
    return input_table({'origin':dht.string,'name':dht.string,'value':dht.string,
        'set_id':dht.string,'run_id':dht.string})
        
def empty_bench_metrics():
    return input_table({'benchmark_name':dht.string,'origin':dht.string,'timestamp':dht.int64,
        'name':dht.string,'value':dht.float64,'note':dht.string, 'set_id':dht.string,
        'run_id':dht.string})

# Load all benchmark-results.csv data collected from the given storage, category, and filters
def load_bench_results(storage_uri, category='adhoc', actor_filter=None, set_filter=None):
    run_ids = get_run_paths(storage_uri, category, actor_filter, set_filter, 100)
    return merge_run_tables(storage_uri, run_ids, category, 'benchmark-results.csv', convert_result)

# Load all benchmark-metrics.csv data collected from the given storage, category, and filters
def load_bench_metrics(storage_uri, category='adhoc', actor_filter=None, set_filter=None):
    run_ids = get_run_paths(storage_uri, category, actor_filter, set_filter, 100)
    return merge_run_tables(storage_uri, run_ids, category, 'benchmark-metrics.csv', convert_metric)

# Load all benchmark-platform.csv data collected from the given storage, category, and filters
def load_bench_platform(storage_uri, category='adhoc', actor_filter=None, set_filter=None):
    run_ids = get_run_paths(storage_uri, category, actor_filter, set_filter, 100)
    return merge_run_tables(storage_uri, run_ids, category, 'benchmark-platform.csv', convert_platform)

# Load all benchmark-results.csv data collected from the given storage, category, and filters by set
# Sets contain one or more runs for each benchmark. This function loads the median run by rate for each benchmark
def load_bench_result_sets(storage_uri, category='adhoc', actor_filter=None, set_filter=None):
    bench_results = load_bench_results(storage_uri,category,actor_filter,set_filter)
    bench_results_sets = bench_results.sort(['benchmark_name','origin','set_id','op_rate']) \
        .group_by(['benchmark_name','origin','set_id']) \
        .view(['benchmark_name','origin','timestamp=(long)mid_item(timestamp)','test_duration=(double)mid_item(test_duration)',
            'set_op_rates=op_rate','op_duration=(double)mid_item(op_duration)','op_rate=(long)mid_item(op_rate)',
            'row_count=(long)mid_item(row_count)','variability=(float)rstd(set_op_rates)','set_id',
            'run_id=(String)mid_item(run_id)','set_count=count(set_op_rates)'])
    # Attach columns for specified metrics and platform properties
    #local_platform_props = []
    #local_metric_props = []
    #bench_results_sets = add_platform_values(bench_results_sets, ['deephaven.version'] + platform_props, local_platform_props)
    #bench_results_sets = add_metric_values(bench_results_sets, metric_props, local_metric_props)
    return bench_results_sets, bench_results
    
def load_table_or_empty(table_name, storage_uri, category='adhoc', actor_filter='', set_filter=''):
    actor = actor_filter.strip(); prefix = set_filter.strip()
    if actor and prefix:
        return globals()[f'load_bench_{table_name}'](storage_uri, category, actor, prefix)
    return globals()[f'empty_bench_{table_name}']()

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

# Format column values for percent or integral depending on the start of the name
def format_columns(table,pct_cols=(),int_cols=()):
    column_formats = []
    for col in table.columns:
        n = col.name
        if n.startswith(pct_cols):
            column_formats.append(n + '=Decimal(`0.0%`)')
        if n.startswith(int_cols):
            column_formats.append(n + '=Decimal(`###,##0`)')
    return table.format_columns(column_formats)

import statistics
# Get a percentage standard deviation for the given list of rates
def rstd(rates) -> float:
    rates = [i for i in rates if i >= 0]
    mean = statistics.mean(rates)
    return (statistics.pstdev(rates) / mean) if mean != 0 else 0.0

# Get the zscore of one rate against a list of rates
def zscore(rate, rates) -> float:
    rates = [i for i in rates if i >= 0]
    std = statistics.pstdev(rates)
    return ((rate - statistics.mean(rates)) / std) if std != 0 else 0.0

# Get the probability that the zscore lacks confidence (lower is better)
def zprob(zscore) -> float:
    lower = -abs(zscore)
    upper = abs(zscore)
    return 1 - (statistics.NormalDist().cdf(upper) - statistics.NormalDist().cdf(lower))

from array import array
# Get the percent change between the last rate in a list and the avg of the previous rates
def rchange(rates) -> float:
    rates = array('l', rates)
    if(len(rates) < 2): return 0.0
    m = statistics.mean(rates[:-1])
    return (rates[-1] - m) / m

# Get the percentage gain between two values
def gain(start, end) -> float:
    return (end - start) / start

# Format a list of rates to make them easier to read in a DHC table
def format_rates(rates):
    return ' '.join("{:,}".format(r) for r in rates)

# Truncate text to the given size. Add '...' for truncated text.
def truncate(text, size):
    if len(text) < size - 3: return text
    return text[:size-3] + '...'

# Get the middle item of the array
def mid_item(arr):
    n = len(arr)
    return arr[n // 2]

# Get the last item of the array
def last_item(arr):
    return arr[-1]

# Merge the elements of an array of arrays into a single typed array
def merge_arrays(type_str, arrs):
    final_arr = []
    for arr in arrs:
        for i in arr.copyToArray():
            final_arr.append(i)
    return jpy.array(type_str, final_arr)


