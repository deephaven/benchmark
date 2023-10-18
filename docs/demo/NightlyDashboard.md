# Nightly Dashboard

Deephaven benchmarks run every night and summary tables (not show here) are published internally so that
developers can see how performance changed during the last nightly build. If more investigation is
needed, the logical next step is to search the benchmark data to get more details.

The Nightly Dashboard allows a quick way to dig into the available data to get some clues to
performance issues or see more clearer why performance has improved.

This Dashboard is built using Deephaven's [scripted UI](https://deephaven.io/core/docs/how-to-guides/plotting/category/)
using the Benchmark Tables snippet used in the previous notebook.

```python
from urllib.request import urlopen; import os

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly
    benchmark_max_runs_arg = 5  # Latest X runs to include
    exec(r.read().decode(), globals(), locals())
```
