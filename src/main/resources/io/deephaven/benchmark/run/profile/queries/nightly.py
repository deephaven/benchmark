# Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between nightly benchmarks
# - Drill into nightly rate gain/loss for a single operations for a date-time range
# Requirements: Deephaven 0.23.0 or greater

from urllib.request import urlopen

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'nightly'  # release | nightly    
    benchmark_max_runs_arg = 20  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())

def op_by_date_range(begin_millis, end_millis, op_prefix):
    interval_millis = 43200000
    begin_interval = int(begin_millis / interval_millis)
    end_interval = int(end_millis / interval_millis)

    return bench_results_diff.where([
        "begin_interval <= (int)(timestamp / interval_millis)", 
        "end_interval >= (int)(timestamp / interval_millis)", 
        "benchmark_name.startsWith(`" + op_prefix + "`)"
    ]).sort([
        'timestamp', 'benchmark_name'
    ]).update([
        'timestamp=epochMillisToInstant(timestamp)'
    ]).group_by([
        'benchmark_name', 'origin'
    ]).view([
        'Benchmark=benchmark_name', 'Start=timestamp[0]', 'End=timestamp[len(timestamp)-1]', 
        'Start_Rate=op_rate[0]', 'End_Rate=op_rate[len(op_rate)-1]',
        'Rate_Change=(float)gain(Start_Rate, End_Rate)',
        'Start_Heap_Used=(long)heap_used[0]', 'End_Heap_Used=(long)heap_used[len(heap_used)-1]',
        'Heap_Used_Change=(float)gain(Start_Heap_Used, End_Heap_Used)',
        'Start_NonHeap_Used=(long)non_heap_used[0]', 'End_NonHeap_Used=(long)non_heap_used[len(non_heap_used)-1]',
        'NonHeap_Used_Change=(float)gain(Start_NonHeap_Used, End_NonHeap_Used)'
    ]).where([
        '!Benchmark.contains(`Rows Per`)'
    ])

last_by_range = op_by_date_range(1693470171012, 1693988753474, 'LastBy-')
