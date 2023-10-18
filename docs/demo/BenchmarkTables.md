# Nightly and Release Benchmark Tables

Deephaven benchmarks run every night on the same (or equivalent) server. The script below
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
from urllib.request import urlopen; import os

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(root + '/deephaven-benchmark/benchmark_tables.dh.py') as r:
    benchmark_storage_uri_arg = root + '/deephaven-benchmark'
    benchmark_category_arg = 'release'  # release | nightly
    benchmark_max_runs_arg = 5  # Latest X runs to include
    exec(r.read().decode(), globals(), locals())
```
The script works with the benchmark data stored on this demo system but also works in any
[Deephaven Community Core](https://deephaven.io/core/docs/) instance that has
internet connectivity. Copy the script and any additions you have made to another
Deephaven notebook and run just as you did here.

## How Does This Python Snippet Work?

The following is a line-by-line walkthrough of what the above script is doing:
1. Import the [_urllib.request_](https://docs.python.org/3/library/urllib.request.html) package and set up _urlopen_ for use
2. Blank
3. Detect the parent location of the benchmark data; local Deephaven data directory or GCloud data directory
4. Open the *benchmark_tables* script from the discovered parent location
5. Tell the *benchmark_tables* script where the benchmark data is
6. Tell the *benchmark_tables* script what set of data to process
7. Tell the *benchmark_tables* script how many benchmark runs to include
8. Execute the *benchmark_tables* script to generate the tables

Script Arguments:
1. *benchmark_storage_uri_arg*: 
   - Where to load benchmark data from (don't change if you want to use Deephaven data storage)
2. *benchmark_category_arg*: 
   - _release_ for benchmarks collected on a specific [Deephaven releases](https://github.com/deephaven/deephaven-core/releases)
   - _nightly_ for benchmarks collected every night
3. *benchmark_max_runs_arg*:
   - The number of benchmark runs to include, starting from latest
