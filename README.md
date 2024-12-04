# Deephaven Benchmark

[Summary of Latest Successful Nightly Benchmarks](docs/NightlySummary.md)
![Operation Rate Change Tracking By Release](https://storage.googleapis.com/deephaven-benchmark/nightly/deephaven/benchmark-summary.svg?)
([See Other Deephaven Summaries Below](#other-deephaven-summaries))

The Benchmark framework provides support for gathering performance measurements and statistics for operations on tabular data.  It uses the JUnit
framework as a runner and works from popular IDEs or from the command line. It is geared towards scale testing interfaces capable of ingesting 
table data, transforming it, and returning tabular results. 

Currently, most benchmarks that use the framework are aimed at broad coverage of single query operations executed in 
[Deephaven Community Core](https://deephaven.io/community/) through the Barrage Java Client. Tests focus on querying static parquet files, 
streamed Kafka topics, and replayed data.

The project maintains several hundred standardized benchmarks for Deephaven query operations that are tracked both from release-to-release and 
nightly. Results are regularly published to a read-only GCloud bucket (*deephaven-benchmark*) available through the public storage API. 

The typical workflow of a Benchmark test is... *Configure table/column generation* --> *Execute Query* --> *Measure Results*.  This is all done inside a JUnit test class.

Tests are designed to scale by changing a scale property value call *scale.row.count*, and per-test scale multipliers, so the same test can be used in multiple runs 
at different scales for comparison.  For ease of comparison, collected results use processing rates for benchmarked operations in addition to elapsed time. MXBean 
metrics are also collected for each benchmark as well as details about the platform where each test ran.

Tests are run client-server, so the test runner does not need to be co-located with the Deephaven Engine. Measurements and statistics are taken directly 
from the engine(s) to reduce the affect of I/O and test setup on the results.

Resources:
- [Getting Started](docs/GettingStarted.md) - Getting set up to run benchmarks against Deephaven Community Core
- [Testing Concepts](docs/TestingConcepts.md) - Understanding what drives Benchmark development
- [Test-writing Basics](docs/TestWritingBasics.md) - How to generate data and use it for tests
- [Collected Results](docs/CollectedResults.md) - What's in the benchmark results
- [Running the Release Distribution](docs/distro/BenchmarkDistribution.md) - How to run Deephaven benchmarks from a release tar file
- [Running from the Command Line](docs/CommandLine.md) - How to run the benchmark jar with a test package
- [Running Adhoc Github Workflows](docs/AdhocWorkflows.md) - Running benchmark sets on-demand from Github
- [Published Results Storage](docs/PublishedResults.md) - How to grab and use Deephaven's published benchmarks
- [Sssh Secrets](docs/GithubSecrets.md) - How to define Github secrets for running Benchmark workflows in a fork

## Other Deephaven Summaries

[Summary of Comparison Benchmarks](docs/ComparisonSummary.md)
![Operation Rate Product Comparison](https://storage.googleapis.com/deephaven-benchmark/compare/deephaven/benchmark-summary.svg?)
