#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Smoke test ready-for-merge benchmark changes. Attempts to do something similar to the test ordering that would
# occur when nightly or release run, then does it the adhoc or compare categories are run. 

cp /home/stan/Git/benchmark/target/deephaven-benchmark*.jar .
rm -rf /home/stan/Data/benchmark/results

rm -f /home/stan/Deephaven/deephaven-edge/data/*.def
rm -f /home/stan/Deephaven/deephaven-edge/data/*.parquet

JAVA_OPTS="-Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-1.0-SNAPSHOT-standalone.jar -cp deephaven-benchmark-1.0-SNAPSHOT-tests.jar"

# Run all benchmarks except tagged then run tagged benchmarks with iterations (Similar to what release and nightly do)
java ${JAVA_OPTS} -p io.deephaven.benchmark.tests.standard -n "^.*[.].*Test.*$"  -T Iterate

ITERATIONS=3
for i in `seq 1 ${ITERATIONS}`
do

echo "*** Starting Iteration: $i ***"
java ${JAVA_OPTS} -p io.deephaven.benchmark.tests.standard -t Iterate

done

# Publish accumulated results for scores to slack
java -Dbenchmark.profile=smoke-test-scale-benchmark.properties -jar deephaven-benchmark-1.0-SNAPSHOT-standalone.jar publish

# Run all benchmarks regardless of tag (Similar to what adhoc and compare do)
rm -f /home/stan/Deephaven/deephaven-edge/data/*.def
rm -f /home/stan/Deephaven/deephaven-edge/data/*.parquet

java ${JAVA_OPTS} -p io.deephaven.benchmark.tests.standard -n "^.*[.].*Test.*$"
