# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between two or more
# adhoc benchmark set runs
# - Make a table containing rates and diffs for the given benchmark sets
# - Expects the following arguments set in globals() before execution
#   - benchmark_sets_arg = []  # The set names to run including user (ex. ['user1/myset1','user1/myset2')
#   - benchmark_set_runs_arg = 5  # Number of runs to load from each set (Can be greater than available)
# Requirements: Deephaven 0.32.0 or greater

from urllib.request import urlopen; import os, re

benchmark_sets_arg = ['stanbrub/v0.34.0','stanbrub/v0.35.0']
benchmark_max_sets_arg = 10

# benchmark_sets_prefix =  os.path.commonprefix(benchmark_sets_arg)
benchmark_sets_prefix = 'stanbrub/v'

result = None
first_set = None
for benchmark_set in benchmark_sets_arg:
    root = 'file:///data' if os.path.exists('/data/deephaven-benchmark') else 'https://storage.googleapis.com'
    with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
        benchmark_storage_uri_arg = root + '/deephaven-benchmark'
        benchmark_category_arg ='adhoc'
        benchmark_actor_filter_arg = os.path.dirname(benchmark_set)
        benchmark_set_filter_arg = os.path.basename(benchmark_set)
        benchmark_metric_props_arg = ['data.file.size']
        exec(r.read().decode(), globals(), locals())
    
    set_name = normalize_name(benchmark_set.replace(benchmark_sets_prefix,''))
    tbl = bench_results_sets.group_by(['benchmark_name']) \
        .view(['Benchmark=benchmark_name',
            'Variability__' + set_name + '=(float)rstd(merge_arrays("long",set_op_rates)) / 100.0',
            'Rate__' + set_name + '=(long)median(op_rate)',
            'DataSize__' + set_name + '=(long)median(data_file_size)'])
    if result is None:
        result = tbl
        first_set = set_name
    else:
        first_rate = 'Rate__' + first_set
        curr_rate = 'Rate__' + set_name
        result = result.join(tbl, on=['Benchmark'], joins=['Variability__' + set_name, curr_rate, 'DataSize__' + set_name])
        result = result.update_view([
            'Change__' + set_name + '=(float)gain(' + first_rate + ',' + curr_rate + ') / 100.0'
        ])

bench_results = bench_metrics = bench_platforms = bench_metrics_diff = None
bench_results_diff = bench_results_change = tbl = None

column_formats = []
for col in result.columns:
    n = col.name
    if n.startswith('Variability') or n.startswith('Change'):
        column_formats.append(n + '=Decimal(`0.0%`)')
    if n.startswith('Rate'):
        column_formats.append(n + '=Decimal(`###,##0`)')

adhoc_set_compare = result.format_columns(column_formats)
result = None

