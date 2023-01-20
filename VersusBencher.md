# Benchmark - Bencher Comparison

Benchmark is a JUnit-based framework for testing queries (or whatever) against Deephaven Engine.  Bencher is a JSON file-based framework for testing queries against Deephaven Engine.  What follows is a comparison of the features of each meant to gain insight into improvements for both.

## Feature Comparison
|Feature|Bencher|Benchmark|
|-------|-------|------|
|Test Query Definition|Specified several JSON configuration files|Specified in JUnit test code|
|Data Table Definition|Specified in several JSON configuration files|Specified in the same JUnit test code as the queries|
|Data Generation for Tests|Generates parquet file directly to Engine data directory|Generates either parquet or kafka records across the network through Engine|
|Fetch Table Results for Verification|Does not fetch or validate table results|Fetches full table updates either after or during query|
|Multi-system Test Running|Requires test runner and Engine on same system & OS|Test runner and Engine can be on different systems or OS's|
|Docker Installs Required|Only Deephaven Engine|Deephaven Engine and Redpanda|
|Debugging Tests|Cannot step through JSON test files|Can step through with Java debugger|
|Scaling to Greater Row Counts|Requires creating new generator and test files|Change scale.row.count in property file. Rerun tests|
|Generated File Reuse|Detects existence of up-to-date parquet file and reuses|Reuses existing parquet files based on generator configuration|
|Generated Data Compression|Writes parquet files with GZIP codec|Write parquet files and produces Kafka records with configurable codec|
|Iterative Test Runs|Iterates query test with one result reported per iteration|Does not run more than once per test|
|Capture System Details|Collects test hardware and VM details|Does not collect any system details|
|Table Column Generation Distributions|Allows multiple ways to randomly distribute column values|
|Table Column String Generation|Import string list hardcoded in files|Generate string with a prefix and numeric range|
|Baselines with Pandas|Runs comparable queries in both Deephaven and Pandas|Does not do Pandas|

## Highlights
Bencher
- Robust data generation with many types of columns and distributions of data
- Broad set of standardized operational tests
- Does baseline tests against Pandas

Benchmark
- Ease of developing and debugging new tests
- Table data configuration and queries in the same place
- Can run on multi-system setups
- Configurable scale for tests