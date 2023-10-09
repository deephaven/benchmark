# Copyright (c) 2022-2023 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between nightly benchmarks
# - Make two tables; one cryptic and small, the other clearer with more rows
# Requirements: Deephaven 0.23.0 or greater

from urllib.request import urlopen; import os
root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'nightly'  # release | nightly    
    benchmark_max_runs_arg = 45  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())

import statistics
def zscore(rate, rates):
    return (rate - statistics.mean(rates)) / statistics.pstdev(rates)

platform_details = bench_platforms.sort_descending(['run_id']).group_by(['run_id']).first_by().ungroup()

nightly_worst_rate_change = bench_results.where([
    'benchmark_name.endsWith(`-Static`)'
]).exact_join(
    bench_platforms.where(['name=`deephaven.version`']),
    on=['run_id', 'origin'], joins=['deephaven_version=value']
).sort_descending([
    'benchmark_name','timestamp','deephaven_version','origin'
]).group_by([
    'benchmark_name','deephaven_version','origin'
]).head_by(2, [
    'benchmark_name','origin'
]).update([
    'past_5_rates_ex=op_rate_[i].subVector(1,6)','past_5_rates_in=op_rate','op_rate=op_rate[0]',
    'avg_rate_ex=avg(past_5_rates_ex)','var_rate_ex=rstd(past_5_rates_ex)',
    'avg_rate_in=avg(past_5_rates_in)','score=(float)zscore(op_rate,past_5_rates_ex)'
]).group_by([
    'benchmark_name','origin'
]).view([
    'Static_Benchmark=benchmark_name.replace(`-Static`,``)',
    'Variability=(float)var_rate_ex[0]/100',
    'Rate=op_rate[0]',
    'Change=(float)gain(avg_rate_ex[0],op_rate[0])/100',
    'Since_Release=(float)gain(avg_rate_in[1],op_rate[0])/100',
    'Score=score[0]'
]).sort([
    'Score'
])

nightly_worst_rate_change_large = nightly_worst_rate_change.head_by(20).format_columns([
    'Variability=Decimal(`0.0%`)','Rate=Decimal(`###,##0`)',
    'Change=Decimal(`0.0%`)','Since_Release=Decimal(`0.0%`)'
])

nightly_worst_rate_change_small = nightly_worst_rate_change.head_by(10).view([
    'Static_Benchmark=Static_Benchmark.substring(0, 20)+`...`',
    'Chng5d=Change','Var5d=Variability','Rate','ChngRls=Since_Release'
]).format_columns([
    'Rate=Decimal(`###,##0`)','Chng5d=Decimal(`0.0%`)','Var5d=Decimal(`0.0%`)',
    'ChngRls=Decimal(`0.0%`)'
])
