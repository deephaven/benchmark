from urllib.request import urlopen; import os
root = 'file:///data' if os.path.exists('/data/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'nightly'  # release | nightly    
    benchmark_max_runs_arg = 45  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())

platform_details = bench_platforms.sort_descending(['run_id']).group_by(['run_id']).first_by().ungroup()

worst_since_last_version = bench_results.where([
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
    'timestamp=timestamp[0]','Variability=(float)rstd(op_rate)','op_rate=op_rate[0]'
]).group_by([
    'benchmark_name','origin'
]).view([
    'Static_Benchmark=benchmark_name.replace(`-Static`,``)',
    'Days=round(diffDays(epochMillisToInstant(timestamp[1]),epochMillisToInstant(timestamp[0])))',
    'Variability=Variability[0]/100','Rate=op_rate[0]','Change=(float)gain(op_rate[1],op_rate[0])/100'
]).sort([
    'Change'
]).head_by(20).format_columns([
    'Variability=Decimal(`0.0%`)','Change=Decimal(`0.0%`)'
])