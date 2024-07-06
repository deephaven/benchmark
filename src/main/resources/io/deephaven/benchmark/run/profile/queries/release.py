# Copyright (c) 2022-2024 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between releases
# - Generate tables for best and wost static rates between the latest release and previous
# - Generate table using same benchmarks as standard summmary SVG for comparison to previous releases
# Requirements: Deephaven 0.23.0 or greater

from urllib.request import urlopen; import os

root = 'file:///data' if os.path.exists('/data/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly
    benchmark_max_sets_arg = 5
    benchmark_actor_filter_arg = 'deephaven'
    benchmark_set_filter_arg = '[0-9]{2}[.][0-9]{3}[.][0-9]{2}'
    exec(r.read().decode(), globals(), locals())

# Replace any characters that are illegal in DH column names
def column_name(name):
    name = name.replace('/','__')
    return re.sub('[^A-Za-z0-9_$]', '_', name)

# Return a table containing only non-obsolete benchmarks having at least two of the most recent versions
# Candidate for pulling up into deephaven_tables.py
def latest_comparable_benchmarks(filter_table):
    latest_benchmark_names = bench_results_sets.view([
        'set_id','benchmark_name'
    ]).group_by(['set_id']).sort_descending(['set_id']).first_by().ungroup()

    new_benchmark_names = bench_results_sets.where_in(
        latest_benchmark_names,['benchmark_name=benchmark_name']
    ).group_by(['benchmark_name']).where(['len(op_rate) < 2']).ungroup()

    results_tbl = filter_table.where_in(
        latest_benchmark_names,['benchmark_name']  # Exclude obsolete benchmarks
    ).where_not_in(
        new_benchmark_names,['benchmark_name']  # Exclude single-version benchmarks
    )
    return results_tbl

newest_benchmarks = latest_comparable_benchmarks(bench_results_sets).sort_descending(['set_id'])

vers_tbl = newest_benchmarks.view(["deephaven_version"])

from deephaven import numpy as dhnp
vers = dhnp.to_numpy(newest_benchmarks.view(["deephaven_version"]).first_by())
print("Vers: ", vers)
versLen = len(vers)
vers = [normalize_name(ver) for ver in vers]

past_static_rates = newest_benchmarks.where(['benchmark_name.endsWith(`-Static`)']) \
    .group_by(['benchmark_name','origin','set_id']) \
    .update(['Change=gain(op_rate[1], op_rate[0])']) \
    .update([(vers[i] + "=op_rate[" + str(i) + "]") for i in range(versLen)]) \
    .view(['Static_Benchmark=benchmark_name.replace(` -Static`,``)',
        'Duration=op_duration[0]','Variability=variability[0]','Change'] + 
        [vers[i] for i in range(versLen)
])

worst_static_rate_changes = past_static_rates.sort(['Change']).head_by(25)
best_static_rate_changes = past_static_rates.sort_descending(['Change']).head_by(25)

summary_bechmark_names = [
    'Where- 2 Filters','Update-Sum- 2 Calcs Using 2 Cols','SelectDistinct- 1 Group 100 Unique Vals',
    'AvgBy- 3 Groups 100K Unique Combos','Sort- 2 Cols Ascending',
    'Join- Join On 2 Cols','CumSum- 1 Group 100 Unique Vals',
    'EmaTime- 2 Groups 10K Unique Combos',
    'RollingSumTick- 2 Groups 10K Unique Combos',
    'RollingGroupTick- 1 Group 100 Unique Vals','AsOfJoin- Join On 2 Cols'
]

summary_benchmarks = past_static_rates.where_one_of([
    ("Static_Benchmark='" + name + "'") for name in summary_bechmark_names
])

parquet_benchmarks = past_static_rates.where(["Static_Benchmark.startsWith(`Parquet`)"]).sort(['Change'])

