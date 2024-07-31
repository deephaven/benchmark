from deephaven.updateby import rolling_group_tick
from urllib.request import urlopen; import os

score_threshold = 10.0

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_max_sets_arg = 100
    benchmark_category_arg ='nightly'
    exec(r.read().decode(), globals(), locals())

def count_threshold_hits(scores) -> int:
    scores = array('d', scores)
    hits = 0
    for score in scores:
        if score <= -score_threshold or score >= score_threshold:
            hits = hits + 1
    return hits

op_day5 = rolling_group_tick(cols=['op_rates=op_rate','set_op_rates=set_op_rates'], rev_ticks=6, fwd_ticks=-1)
scores = bench_results_sets.sort_descending(['benchmark_name','set_id']) \
    .update_by(ops=[op_day5], by="benchmark_name") \
    .where(['op_rates.size() > 2']) \
    .update(['set_op_rates=(long[])merge_arrays(`long`,set_op_rates)','score=zscore(op_rate, op_rates)',
        'var5d=rstd(op_rates)']) \
    .group_by(['benchmark_name']) \
    .update(['op_rate=avg(op_rate)','var5d=avg(var5d)','score_min=min(score)','score_max=max(score)',
        'score_avg=avg(score)','hits=count_threshold_hits(score)']) \
    .view(['Benchmark=benchmark_name','Rate=op_rate','Var5d=var5d','ScoreMin=score_min',"ScoreMax=score_max",
        'ScoreAvg=score_avg','Hits=hits'
    ])

