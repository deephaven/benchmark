from urllib.request import urlopen; import os

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly    
    benchmark_max_runs_arg = 2  # Latest X runs to include   
    exec(r.read().decode(), globals(), locals())
