#!/bin/bash

# This script takes deephaven-benchmark directory (like those stored in the GCloud bucket) and minimizes
# them for use in integration tests that are checked into the project

keep_benchmarks () {
  for f in `find it-deephaven-benchmark -name "$1"`; do
    echo "Keeping Benchmarks in $f"
    cp $f $f.bak
    grep -E "benchmark_name,|ParquetWrite- LZ4 2 Strs 2 Longs 2 Dbls -Static|ParquetRead- LZ4 2 Strs 2 Longs 2 Dbls -Static|ParquetWrite- Lz4Raw Multi Col -Static|ParquetRead- Lz4Raw Multi Col -Static|VarBy- 2 Group 160K Unique Combos Float -Static|WhereNotIn- 1 Filter Col -Static|Where- 2 Filters -Static|Vector- 5 Calcs 1M Groups Dense Data -Static|WhereOneOf- 2 Filters -Static|WeightedSum-AggBy- 3 Sums 2 Groups 160K Unique Vals -Static|CumCombo- 6 Ops No Groups -Static|SelectDistinct- 1 Group 250 Unique Vals -Static|AsOfJoin- Join On 2 Cols 1 Match -Static|AsOfJoin- Join On 2 Cols 1 Match -Inc" $f > $f.out
    mv $f.out $f
  done
}

keep_metrics () {
  for f in `find it-deephaven-benchmark -name "$1"`; do
    echo "Keeping Metrics In $f"
    grep -E "benchmark_name,|HeapMemoryUsage Used|HeapMemoryUsage Committed|G1 Young Generation|G1 Old Generation" $f > $f.out
    mv $f.out $f
  done
}

keep_benchmarks benchmark-results.csv
keep_benchmarks benchmark-metrics.csv
keep_metrics benchmark-metrics.csv
