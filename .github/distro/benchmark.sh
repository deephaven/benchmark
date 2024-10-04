#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending
#
# This script shows an example of how to run the Deephaven operational benchmarks against
# the Docker Deephaven in this directory. Each run will purge results and generated data from
# any previous run and produce results as a set of csv files for each iteration.
#
# Examples:
# - Run Where benchmarks three times: ./benchmark.sh 1 Where 
# - Run Where and AvgBy benchmarks three times: ./benchmark.sh 3 "Where,Avg*"
# - Run all benchmarks one time: ./benchmark.sh 1 *
#
# Notes:
# - Docker is a prerequisite, and the Deephaven image will be installed if it's missing
# - Available benchmarks for the test class list are test class simple names (no package)
# - Running a full set (*) at default scale will take several hours or more
# - Use the benchmark.properties file to change scale.row.count for higher/lower scale
# - Benchmark metrics are in the results directory

if [[ $# != 2 ]]; then
    echo "$0: Missing iteration count or test class list argument"
    exit 1
fi

ITERATIONS=$1
TEST_WILD=$2
BENCH_MAIN="io.deephaven.benchmark.run.BenchmarkMain"
TEST_PACKAGE="io.deephaven.benchmark.tests.standard"

sudo docker compose down

rm -rf ./results
sudo rm -f ./data/*.def
sudo rm -f ./data/*.parquet

sudo docker compose up -d 

TEST_REGEX="^.*[.]("
for r in $(echo ${TEST_WILD} | sed 's/\s*,\s*/ /g'); do
  TEST_REGEX="${TEST_REGEX}"$(echo "(${r}Test)|" | sed 's/\*/.*/g')
done
TEST_REGEX=$(echo ${TEST_REGEX} | sed -E 's/\|+$//g')
TEST_REGEX="${TEST_REGEX})$"

for i in `seq 1 ${ITERATIONS}`
do

echo "*** Starting Iteration: $i ***"

java -Dbenchmark.profile=benchmark.properties -cp "libs/*" ${BENCH_MAIN} -p ${TEST_PACKAGE} -n "${TEST_REGEX}" 

done

sudo docker compose down

