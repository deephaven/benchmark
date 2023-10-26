# Workflow Integration Test Directory

This workflow directory contains curated data for integration tests and a script to help
transform real benchmark data (e.g. GCloud bucket data) into something useful for the
tests.  The script is not necessary to run unless new data is required for a test.

Care must be taken to understand the tests' use cases before changing any existing data.
For example, the data may need to simulate benchmarks that are new and do not have results
for a previous version.


## Required Benchmark Metrics
The following metrics are currently required when running the base table query snippet. They 
must be included in the _benchmark-metrics.csv_ files that are staged for the workflow tests.

```
add_metric_value_diff('MemoryImpl', 'Memory', 'HeapMemoryUsage Used', "heap_used")
add_metric_value_diff('MemoryImpl', 'Memory', 'NonHeapMemoryUsage Used', "non_heap_used")
add_metric_value_diff('MemoryImpl', 'Memory', 'HeapMemoryUsage Committed', 'heap_committed')
add_metric_value_diff('MemoryImpl', 'Memory', 'NonHeapMemoryUsage Committed', 'non_heap_committed')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Young Generation', 'CollectionCount', 'g1_young_collect_count')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Young Generation', 'CollectionTime', 'g1_young_collect_time')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Old Generation', 'CollectionCount', 'g1_old_collect_count')
add_metric_value_diff('GarbageCollectorExtImpl', 'G1 Old Generation', 'CollectionTime', 'g1_old_collect_time')
```

## Benchmarks Used for Test Coverage
The data staged on the Deephaven server covers three main cases
- Obsolete: Benchmarks that have history but are not longer used
- Recently Added: Benchmarks that have more than one run but were not run in the previous Deephaven version
- New: Benchmarks that only have one run
- Existing: Benchmarks that have run in the current and previous versions

**Obsolete**                                    
- ParquetWrite- LZ4 2 Strs 2 Longs 2 Dbls -Static
- ParquetRead- LZ4 2 Strs 2 Longs 2 Dbls -Static

**Recently Added**
- ParquetWrite- Lz4Raw Multi Col -Static

**New**
- ParquetRead- 1 String Col -Static

**Filtered Out**
- AsOfJoin- Join On 2 Cols 1 Match -Inc

**Rate Outlier**
- AsOfJoin- Join On 2 Cols 1 Match -Static

**Existing**
- VarBy- 2 Group 160K Unique Combos Float -Static
- WhereNotIn- 1 Filter Col -Static
- Where- 2 Filters -Static
- Vector- 5 Calcs 1M Groups Dense Data -Static
- WhereOneOf- 2 Filters -Static
- CumCombo- 6 Ops No Groups -Static
- SelectDistinct- 1 Group 250 Unique Vals -Static

