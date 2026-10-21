# Running Adhoc Workflows

In addition to the benchmarks that are run nightly and after every release, developers can run adhoc benchmarks. These benchmarks can be configured to run small sets of standard benchmarks on-demand. This is useful for more targeted comparisons between multiple sets of [Deephaven Community Core](https://deephaven.io/community/) versions or configuration options.

A common practice is to run a comparison from a source branch that is ready for review to the main branch for a subset of relevant benchmarks (e.g. Parquet). This allows developers to validate the performance impact of code changes before they are merged. Other possibilities include comparing JVM options for the same DHC version, comparing data distributions (e.g. ascending, descending), and comparing levels of data scale.

All results are stored according to the initiating user and a user-supplied label in the public [Benchmarking GCloud bucket](https://console.cloud.google.com/storage/browser/deephaven-benchmark). Though the results are available through public URLs, Google Cloud browsing is not. Retrieval of the generated data is mainly the domain of the Adhoc Dashboard.

Prerequisites:
- Permission to use Deephaven's Bare Metal servers and Github Secrets
- An installation of a [Deephaven Community Core w/ Python docker image](https://deephaven.io/core/docs/getting-started/docker-install/) (0.36.1+)
- The Adhoc Dashboard python snippet shown in this guide
- Access to the [Benchmark Workflow Actions](https://github.com/deephaven/benchmark/actions)

### Starting a Benchmark Run

The typical Benchmark run will be initiated from a [Github on-demand workflow](https://docs.github.com/en/actions/managing-workflow-runs-and-deployments/managing-workflow-runs/manually-running-a-workflow) action in the main [Deephaven Benchmark](https://github.com/deephaven/benchmark/actions). However, the "Adhoc" workflows can be run from a fork as well, assuming the correct privileges are set up. (This is outside the scope of this document.) 

There are two Adhoc Workflows:
- [Adhoc Benchmarks (Auto-provisioned Server)](https://github.com/deephaven/benchmark/actions/workflows/adhoc-auto-remote-benchmarks.yml)
- [Adhoc Benchmarks (Nightly Server)](https://github.com/deephaven/benchmark/actions/workflows/adhoc-exist-remote-benchmarks.yml)

Of the two workflows, privileged users will use the "Auto-provisioned Server" workflow in the vast majority of cases. The "Nightly Server" workflow runs on the always-on server shared with the nightly benchmarks, so it suits short runs. Longer runs belong in a matrix config run by "Matrix Benchmarks (Matrix Server)".

### Common Workflow UI Fields

The ui fields used for both Adhoc workflows that are common are defined below:
- Use workflow from
  - From the workflow dropdown, select the branch where the desired benchmarks are. This is typically "main" but could be a branch in a fork
- Deephaven Image or Core Branch
  - The [Deephaven Core](https://github.com/deephaven/deephaven-core) branch, commit hash, tag, or docker image/sha
  - ex. Branch: `deephaven:main or myuser:mybranch`
  - ex. Commit: `efad062e5488db50221647b63bd9b38e2eb2dc5a`
  - ex. Tag: `v42.0`
  - ex. Docker Image: `42.0`
  - ex. Docker Sha: `edge@sha256:bba0344347063baff39c1b5c975573fb9773190458d878bea58dfab041e09976`
- Benchmark Test Classes
  - Wildcard names of available test classes. For example, `Avg*` will match the AvgByTest
  - Because of the nature of the benchmark runner, there is no way to select individual tests by name
  - Test classes can be found under the [standard test directory](https://github.com/deephaven/benchmark/tree/main/src/it/java/io/deephaven/benchmark/tests/standard)
- Benchmark Iterations
  - The number of iterations to run for each benchmark. Be careful, large numbers may take hours or days
  - Given that the Adhoc Dashboard uses medians, any even numbers entered here will be incremented
- Benchmark Scale Row Count
  - The number of millions of rows for the base row count
  - All standard benchmarks are scaled using this number. The default is 10
- Benchmark Data Distribution
  - The distribution the data is generated to follow for each column's successive values
  - random: random symmetrical data distributed around and including 0 (e.g. -4, -8, 0, 1, 5)
  - ascending: positive numbers that increase (e.g. 1, 2, 3, 4, 5)
  - descending: negative numbers that decrease (e.g. -1, -2, -3, -4, -5)
  - runlength: numbers that repeat (e.g. 1, 1, 1, 2, 2, 2, 3, 3, 3)

### Adhoc Benchmarks (Auto-provisioned Server)

The auto-provisioned adhoc workflow allows developers to run workflows on bare metal server hardware that is provisioned on the fly for the benchmark run. It requires two branches, tags, commit hashes, or docker images/shas to run for the same benchmark set. This is the workflow most commonly used to compare performance between a Deephaven PR branch and the main branch.

Workflow fields not shared with the Nightly Server workflow:
- Set Label Prefix
  - The prefix used to make the Set Label for each side of the benchmark comparison
  - ex. Setting `myprefix` with the images `41.0` and `42.0` for Deephaven Image or Core Branch cause two directories in the GCloud benchmark bucket
    - `adhoc/githubuser/myprefix_41_0` and `adhoc/githubuser/myprefix_42_0`
    - Because of naming rules, non-alpha-nums will be replaced with underscores

### Adhoc Benchmarks (Nightly Server)

The adhoc workflow that uses the nightly server allows developers more freedom to experiment with JVM options. It also gives them more freedom to shoot themselves in the foot. For example, if max heap is set bigger than the test server memory, the Deephaven service will crash.

Workflow fields not shared with the Auto-provisioned Server workflow:
- Deephaven JVM Options
  - Options that will be included as JVM arguments to the Deephaven service
  - ex. `-Xmx24g -DQueryTable.memoizeResults=false`
  - Add `-Dbench.jfr=profile` to record JFR for each benchmark (see [JFR Recordings](#jfr-recordings))
- Set Label
  - The label to used to store the result in the GCloud benchmark bucket
  - ex. Setting `mysetlabel` would be stored at `adhoc/mygithubuser/mysetlabel`
- Benchmark Test Package
  - The java package where the desired benchmark test classes are
  - Unless making custom tests in a fork, use the default

### JFR Recordings

Setting `-Dbench.jfr=<settings>` in the Deephaven JVM Options (or a matrix row's `config_options`) records a Java Flight Recording of the engine for each benchmark and uploads it with the other results.
- Values name the JDK's JFR settings, and leaving the property out means no recording
  - `profile`: More detail, more overhead
  - `default`: Less detail, less overhead
  - Any other value fails every benchmark with `Could not locate configuration with name ...`
- Recordings are added to the run's `test-logs.tgz`, next to each test class's `query.md` and `engine.log`, and named with the exact `benchmark_name` from the results
  - ex. `adhoc/mygithubuser/mysetlabel/run-1BzVb5hqX/test-logs.tgz`
  - containing `standard.sort.SortComboTest/Sort- 3 Cols -Static.jfr`
- Each recording covers the engine from the start of the benchmark's session (including table generation) to the end of the measured operation, up to 512MB
- JFR changes what it measures. Confirm a performance difference without it before trusting a profiled comparison
- Recordings started with your own JVM options (e.g. `-XX:StartFlightRecording=filename=/data/...`) still work for experiments, but are not collected or uploaded. They stay in the server's `deephaven/data` directory until the next workflow clears the server, so copy them off with `scp` before then

The bucket deletes test logs, with or without recordings, after 14 days. The expiration is a bucket lifecycle rule defined in [gcloud-bucket-lifecycle.json](../.github/resources/gcloud-bucket-lifecycle.json). It is applied by a bucket admin, not by the workflows. Applying it replaces any existing lifecycle rules, so check those first.
```
gcloud storage buckets describe gs://deephaven-benchmark --format="default(lifecycle_config)"
gcloud storage buckets update gs://deephaven-benchmark --lifecycle-file=.github/resources/gcloud-bucket-lifecycle.json
```
  
### The Adhoc Dashboard

The Adhoc Dashboard provides visualization for Benchmark results using Deephaven UI. The typical use case runs a set of benchmarks through the auto-provisioned workflow, and then the two result sets are compared using the dashboard. Results that are displayed include rate comparisons, a rate chart for the runs in each benchmark set, version and platform changes between each set, and basic metrics take before and after each measured run. 

The Adhoc Dashboard is not intended to be bullet proof. It is expected that you know what values to fill in to the dashboard input, and there are no prompts or validation. There are also no "waiting" icons when waiting for data to be download from GCloud.

### Running Adhoc Dashboard

For ease of running on different Deephaven service locations, the snippet below is provided, which downloads and runs the Adhoc Dashboard remotely. This this snippet in DHC code studio.
```
from urllib.request import urlopen; import os

root = 'file:///nfs' if os.path.exists('/nfs/deephaven-benchmark') else 'https://storage.googleapis.com'
with urlopen(f'{root}/deephaven-benchmark/adhoc_dashboard.dh.py') as r:
    exec(r.read().decode(), globals(), locals())
    storage_uri = f'{root}/deephaven-benchmark'
```
This script will bring up a Deephaven UI dashboard separated generally into quadrants that are blank. To view results, use the following prescription:
- Input Text Fields
  - Actor: Fill in your user (the one that ran the Adhoc workflow)
  - Set Label: A portion of the set label (or prefix) supplied during the workflow to match at least one Benchmark Set
  - Click the Apply button, and a table should be loaded underneath the input form
- Benchmark Results Table
  - `Var_` column: A percentage deviation (variability) for the mean for the Benchmark Set runs
  - `Rate_` column: The number of rows per second processed for the benchmark
  - `Change_` column: The gain (+/-) of the rate compared to the first rate from the left
- Benchmark Rate Chart
  - Click a benchmark row in the table
  - A line chart appear showing the runs that make up each set
  - Each series represents a Benchmark Set
- Benchmark Metric Charts
  - Click a benchmark row in the table
  - Dropdowns in the lower left quadrant will be populated with metrics
  - Select a metrics of interest
  - Compare to another series (e.g. Benchmark Set) or to the line chart on the left
- Dependency Diffs
  - In the upper right, there are tabs showing various differences between the DHC versions/bits run
  - Python Changes: Differences in module versions, additions, or subtractions
  - Jar Changes: Differences in jar library versions, additions, or subtractions

The most Common Error Occurs when entering Actor and Set Label values that do not match anything
```
DHError
merge tables operation failed. : RuntimeError: java.lang.IllegalArgumentException: No non-null tables provided to merge
```
In this case, check the Actor/User and Set Labels (or Prefix) values used for the Adhoc Workflow and try again.
