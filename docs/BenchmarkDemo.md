# Nightly and Release Benchmark Tables

Deephaven benchmarks run every night on the same (or eqivalent) server. The script below
generates tables using the latest benchmark data.

Run the script and you will see tables that show the raw results and metrics,
differences between benchmark runs, and differences in metrics before and after
each operation has run.

Explore further by creating your own notebook, copying the script into it, and
running it. Then try using the resulting tables to
[generate your own](https://deephaven.io/core/docs/reference/cheat-sheets/cheat-sheet/),
visualize table data
with [ChartBuilder](https://deephaven.io/core/docs/how-to-guides/user-interface/chart-builder/),
or experiment with [the scripted UI](https://deephaven.io/core/docs/how-to-guides/plotting/category/).

```python
from urllib.request import urlopen

root = 'file:///data' if os.path.exists('/data/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly
    benchmark_max_runs_arg = 5  # Latest X runs to include
    exec(r.read().decode(), globals(), locals())
```

The script works with the data stored on this demo system but also works in any
[Deephaven Community Core](https://deephaven.io/core/docs/) instance that has
internet connectivty. Copy the script and any additions you have made to another
Deephaven notebook and run just as you did here.

