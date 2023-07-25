# Nightly Benchmark Summary

![Operation Rate Change Tracking By Release](https://storage.googleapis.com/deephaven-benchmark/nightly/benchmark-summary.svg)

## Summary Table Organization

- Common operations are shown first followed by less common operations
- Benchmarks are taken for each operation twice; Static and Ticking
  - [Static](https://deephaven.io/core/docs/how-to-guides/data-import-export/parquet-flat): Parquet data is read into memory and made
  available to the operation as a whole
  - [Ticking](https://deephaven.io/core/docs/conceptual/deephaven-overview/): Data is released incrementally each cycle 
- The Operation Benchmark date shows the day when the benchmarks where collected, which is the latest successful run

## Basic Benchmark Methodology

- Run on equivalent hardware every night
- Load test data into memory before each run to normalized results without disk and network I/O
- Scale the data (row count) to target operation run time at around 10 seconds
  - This is not always possible given the speed of some operations versus benchmark hardware constraints
- Always report a consistent unit for the result like rows processed per second

## Digger Deeper into the Demo

A [Benchmark Demo](https://controller.try-dh.demo.community.deephaven.io/) is provided on the Deephaven Code Studio
demo cluster. Among other Demo notebooks is a Benchmark notebook (TBD) that provides scripts to generate comparative 
benchmark and metric tables using a copy of the latest benchmark results. Users can then experiment with Deephaven 
operations and charting visualize the data.