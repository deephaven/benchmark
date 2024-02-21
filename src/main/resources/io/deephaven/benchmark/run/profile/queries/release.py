# Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between releases
# - Generate tables for best and wost static rates between the latest release and previous
# - Generate table using same benchmarks as standard summmary SVG for comparison to previous releases
# Requirements: Deephaven 0.23.0 or greater

from urllib.request import urlopen; import os

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly    
    benchmark_max_runs_arg = 5  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())

# Replace any characters that are illegal in DH column names
def column_name(name):
    name = name.replace('/','__')
    return re.sub('[^A-Za-z0-9_$]', '_', name)

# Return a table containing only non-obsolete benchmarks having at least two of the most recent versions
# Candidate for pulling up into deephaven_tables.py
def latest_comparable_benchmarks(filter_table):
    latest_benchmark_names = bench_results.view([
        'run_id','benchmark_name'
    ]).group_by(['run_id']).sort_descending(['run_id']).first_by().ungroup()

    new_benchmark_names = bench_results.where_in(
        latest_benchmark_names,['benchmark_name=benchmark_name']
    ).group_by(['benchmark_name']).where(['len(op_rate) < 2']).ungroup()

    results_tbl = filter_table.where_in(
        latest_benchmark_names,['benchmark_name']  # Exclude obsolete benchmarks
    ).where_not_in(
        new_benchmark_names,['benchmark_name']  # Exclude single-version benchmarks
    )
    return results_tbl

newest_benchmarks = latest_comparable_benchmarks(bench_results_change).sort_descending(['timestamp']).first_by(['benchmark_name'])

from deephaven import numpy as dhnp

versions = {}
version_vectors = dhnp.to_numpy(newest_benchmarks.view(["op_group_versions"]))
for vector in version_vectors:
    for vers in vector:
        for v in vers.toArray():
            versions['V_' + column_name(v)] = 0
vers = list(versions.keys())
vers.reverse()
versLen = len(vers)

past_static_rates = newest_benchmarks.where([
    'benchmark_name.endsWith(`-Static`)'
]).update([
    'op_group_rates=vec(reverse(op_group_rates)).subVector(0, versLen)',
    'op_group_versions=vecObj(reverseObj(op_group_versions)).subVector(0, versLen)',
    'Change=gain(op_group_rates[1], op_group_rates[0])'
]).update([
    (vers[i] + "=op_group_rates[" + str(i) + "]") for i in range(versLen)
]).view([
    'Static_Benchmark=benchmark_name.replace(` -Static`,``)',
    'Duration=op_duration','Variability=op_rate_variability','Change'] + [
    vers[i] for i in range(versLen)
])

worst_static_rate_changes = past_static_rates.sort(['Change']).head_by(25)
best_static_rate_changes = past_static_rates.sort_descending(['Change']).head_by(25)

summary_bechmark_names = [
    'Where- 2 Filters','Update- 2 Calcs Using Int','SelectDistinct- 1 Group 250 Unique Vals',
    'AvgBy- 2 Groups 160K Unique Combos Int','Sort- 2 Cols Default Order',
    'Join- Join On 2 Cols 1 Match','CumSum- 1 Group 100 Unique Vals 2 Cols',
    'EmaTime- 2 Groups 15K Unique Combos 1 Col Int',
    'RollingSumTick- 3 Ops 2 Groups 15K Unique Combos Int',
    'RollingGroupTick- 3 Ops 1 Group 100 Unique Vals','AsOfJoin- Join On 2 Cols 1 Match'
]

summary_benchmarks = past_static_rates.where_one_of([
    ("Static_Benchmark='" + name + "'") for name in summary_bechmark_names
])

parquet_benchmarks = past_static_rates.where(["Static_Benchmark.startsWith(`Parquet`)"]).sort(['Change'])

