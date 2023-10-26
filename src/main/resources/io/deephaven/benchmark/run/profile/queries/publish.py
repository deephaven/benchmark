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
def rstd(rates):
    rates = [i for i in rates if i >= 0]
    mean = statistics.mean(rates)
    return (statistics.pstdev(rates) * 100.0 / mean) if mean != 0 else 0.0
    
def zscore(rate, rates):
    rates = [i for i in rates if i >= 0]
    std = statistics.pstdev(rates)
    return ((rate - statistics.mean(rates)) / std) if std != 0 else 0.0

def zprob(zscore):
    lower = -abs(zscore)
    upper = abs(zscore)
    return 1 - (statistics.NormalDist().cdf(upper) - statistics.NormalDist().cdf(lower))

# Used to provide platform (e.g. hardware, jvm version) for SVG footer during publish
platform_details = bench_platforms.sort_descending(['run_id']).group_by(['run_id']).first_by().ungroup()

# Ensure that deleted benchmarks are not included in the scores
latest_benchmark_names = bench_results.view([
    'epoch_day=(int)(timestamp/1000/60/60/24)','benchmark_name'
]).group_by(['epoch_day']).sort_descending(['epoch_day']).first_by().ungroup()

# Ensure the newest benchmarks, which have no past data, are not included in scores
new_benchmark_names = bench_results.where_in(
    latest_benchmark_names,['benchmark_name=benchmark_name']
).group_by(['benchmark_name']).where(['len(op_rate) < 2']).ungroup()

# Get benchmarks that have enough data to compare multiple days
bench_results = bench_results.where_in(
    latest_benchmark_names,['benchmark_name']
).where_not_in(
    new_benchmark_names,['benchmark_name']
)

# Get static benchmarks and compare to last 5 days and previous release
nightly_score = bench_results.where([
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
]).group_by([
    'benchmark_name','origin'
]).update([
    'all_rates=vec(concat(op_rate[0],op_rate[1]))',  
    'all_past_rates=all_rates.subVector(1,len(all_rates))',
    'past_5_rates=all_past_rates.subVector(0,6)',
    'last_5_prev_vers_rates=ifelseObj(op_rate[1]!=null,op_rate[1],op_rate[0]).subVector(0,6)',
    'op_rate=all_rates[0]','var_rate=(float)rstd(past_5_rates)',
    'avg_rate=avg(past_5_rates)','prev_vers_avg_rate=avg(last_5_prev_vers_rates)',
    'score=(float)zscore(op_rate,past_5_rates)',
    'prev_vers_score=(float)zscore(op_rate,last_5_prev_vers_rates)',
    'prob=(float)zprob(score)'
])

nightly_worst_score_large = nightly_score.head_by(20).view([
    'Static_Benchmark=benchmark_name.replace(` -Static`,``)',
    'Variability=(float)var_rate/100','Rate=op_rate',
    'Change=(float)gain(avg_rate,op_rate)/100',
    'Since_Release=(float)gain(prev_vers_avg_rate,op_rate)/100',
    'Score=score','Score_Prob=prob'
]).sort([
    'Score'
]).format_columns([
    'Variability=Decimal(`0.0%`)','Rate=Decimal(`###,##0`)',
    'Change=Decimal(`0.0%`)','Since_Release=Decimal(`0.0%`)','Score_Prob=Decimal(`0.00%`)'
])

nightly_worst_score_small = nightly_worst_score_large.head_by(10).view([
    'Static_Benchmark=truncate(Static_Benchmark,50)','Chng5d=Change',
    'Var5d=Variability','Rate','ChngRls=Since_Release','ScrProb=Score_Prob'
]).format_columns([
    'Rate=Decimal(`###,##0`)','Chng5d=Decimal(`0.0%`)','Var5d=Decimal(`0.0%`)',
    'ChngRls=Decimal(`0.0%`)','ScrProb=Decimal(`0.00%`)'
])

bench_results = bench_metrics = bench_platforms = bench_metrics_diff = None
bench_results_change = bench_results_diff = None
