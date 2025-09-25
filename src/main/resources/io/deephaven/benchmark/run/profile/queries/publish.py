# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending 
#
# Supporting Deephaven queries to use the benchmark_snippet to investigate changes between nightly benchmarks
# - Make two tables; one cryptic and small, the other clearer with more rows
# Requirements: Deephaven 0.23.0 or greater

from urllib.request import urlopen; import os
root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'nightly'  # release | nightly
    benchnark_max_sets_arg = 45
    benchmark_actor_filter_arg = 'deephaven'
    benchmark_set_filter_arg = '[0-9]{4}([-][0-9]{2}){2}'  # yyyy-mm-dd
    exec(r.read().decode(), globals(), locals())

# Used to provide platform (e.g. hardware, jvm version) for SVG footer during publish
platform_details = bench_platforms.sort_descending(['set_id']).group_by(['set_id']).first_by().ungroup()

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
    
bench_results = latest_comparable_benchmarks(bench_results_sets)

# Get static benchmarks and compare to last 5 days and previous release
nightly_score = bench_results.sort_descending(['benchmark_name','timestamp','deephaven_version','origin']) \
    .group_by(['benchmark_name','deephaven_version','origin']) \
    .head_by(2, ['benchmark_name','origin']) \
    .group_by(['benchmark_name','origin']) \
    .update(['all_rates=vec(concat(op_rate[0],op_rate[1]))',
        'all_past_rates=all_rates.subVector(1,len(all_rates))',
        'past_5_rates=all_past_rates.subVector(0,6)', 
        'last_5_prev_vers_rates=ifelseObj(op_rate[1]!=null,op_rate[1],op_rate[0]).subVector(0,6)',
        'op_rate=all_rates[0]','var_rate=rstd(past_5_rates)',
        'avg_rate=avg(past_5_rates)','prev_vers_avg_rate=avg(last_5_prev_vers_rates)',
        'score=zscore(op_rate,past_5_rates)',
        'prev_vers_score=zscore(op_rate,last_5_prev_vers_rates)',
        'prob=zprob(score)'])

nightly_worst_score_large = nightly_score \
    .view(['Benchmark=benchmark_name',
        'Variability=var_rate/100','Rate=op_rate',
        'Change=gain(avg_rate,op_rate)/100',
        'Since_Release=gain(prev_vers_avg_rate,op_rate)/100',
        'Score=score','Score_Prob=prob']) \
    .sort(['Score']) \
    .head(20) \
    .format_columns(['Variability=Decimal(`0.0%`)','Rate=Decimal(`###,##0`)',
        'Change=Decimal(`0.0%`)','Since_Release=Decimal(`0.0%`)',
        'Score=Decimal(`0.0`)','Score_Prob=Decimal(`0.00%`)'])

def benchname(name) -> str:
    btype = 'S' if name.endswith('-Static') else 'I'
    name = name.replace(' -Static','').replace(' -Inc','')
    name = truncate(name, 50)
    return btype  + ' ' + name

nightly_worst_score_small = nightly_worst_score_large \
    .head(10) \
    .view(['Benchmark=benchname(Benchmark)','Chng5d=Change',
        'Var5d=Variability','Rate','ChngRls=Since_Release','Scr=Score','ScrProb=Score_Prob']) \
    .format_columns(['Rate=Decimal(`###,##0`)','Chng5d=Decimal(`0.0%`)','Var5d=Decimal(`0.0%`)',
        'ChngRls=Decimal(`0.0%`)','Scr=Decimal(`0.0`)','ScrProb=Decimal(`0.00%`)'])

nightly_best_score_large = nightly_score \
    .view(['Benchmark=benchmark_name',
        'Variability=var_rate/100','Rate=op_rate',
        'Change=gain(avg_rate,op_rate)/100',
        'Since_Release=gain(prev_vers_avg_rate,op_rate)/100',
        'Score=score','Score_Prob=prob']) \
    .sort_descending(['Score']) \
    .head(20) \
    .format_columns(['Variability=Decimal(`0.0%`)','Rate=Decimal(`###,##0`)',
        'Change=Decimal(`0.0%`)','Since_Release=Decimal(`0.0%`)',
        'Score=Decimal(`0.0`)','Score_Prob=Decimal(`0.00%`)'])

nightly_best_score_small = nightly_best_score_large \
    .head(10) \
    .view(['Benchmark=benchname(Benchmark)','Chng5d=Change',
        'Var5d=Variability','Rate','ChngRls=Since_Release','Scr=Score','ScrProb=Score_Prob']) \
    .format_columns(['Rate=Decimal(`###,##0`)','Chng5d=Decimal(`0.0%`)','Var5d=Decimal(`0.0%`)',
        'ChngRls=Decimal(`0.0%`)','Scr=Decimal(`0.0`)','ScrProb=Decimal(`0.00%`)'])

bench_results = bench_metrics = bench_platforms = bench_metrics_diff = None
bench_results_change = bench_results_diff = None

