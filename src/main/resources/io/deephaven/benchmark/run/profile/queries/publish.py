from urllib.request import urlopen; import os
root = 'file:///data' if os.path.exists('/data/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'nightly'  # release | nightly    
    benchmark_max_runs_arg = 45  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())

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
    'avg_rate_in=avg(past_5_rates_in)'
]).group_by([
    'benchmark_name','origin'
]).view([
    'Static_Benchmark=benchmark_name.replace(`-Static`,``)',
    'Variability=(float)var_rate_ex[0]/100',
    'Rate=op_rate[0]',
    'Change=(float)gain(avg_rate_ex[0],op_rate[0])/100',
    'Since_Release=(float)gain(avg_rate_in[1],op_rate[0])/100'
]).sort([
    'Change'
]).head_by(20).format_columns([
    'Variability=Decimal(`0.0%`)','Rate=Decimal(`###,##0`)',
    'Change=Decimal(`0.0%`)','Since_Release=Decimal(`0.0%`)'
])
