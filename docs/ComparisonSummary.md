# Comparison Benchmark Summary

![Operation Rate Comparison](https://storage.googleapis.com/deephaven-benchmark/compare/deephaven/benchmark-summary.svg?)

## Comparison Table Organization

- Each row shows the benchmark in rows per sec for equivalent operations
- The versions displayed show the latest stable release of each product
- The Benchmark Date shows the day when the benchmarks where collected

## Basic Benchmark Methodology

- All products are run on the same hardware in the same environment
- For each operation (row), all products use the same single parquet file
- All products are benchmarked "out of the box" with no tuning
- Parquet read is included in the benchmark
   - Some products group or index on parquet read
- Deephaven runs as a service while the other products run in-process

