#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Run benchmarks on the remote side
# Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 6 ]]; then
  echo "$0: Missing run type, test package, test regex, row count, distribution, or iterations argument"
  exit 1
fi

RUN_TYPE=$1
TEST_PACKAGE=$2
TEST_PATTERN="$3"
ROW_COUNT=$4
DISTRIB=$5
ITERATIONS=$6
TAG_ITERS=4
HOST=`hostname`
RUN_DIR=/root/run
DEEPHAVEN_DIR=/root/deephaven

if [ ! -d "${RUN_DIR}" ]; then
  echo "$0: Missing the Benchmark run directory"
  exit 1
fi

title () { echo; echo $1; }

title "- Running Remote Benchmark Artifact on ${HOST} -"

title "-- Setting up for Benchmark Run --"

cd ${DEEPHAVEN_DIR};
docker compose down
rm -f data/*.*
docker compose up -d
sleep 10

title "-- Running Benchmarks --"
cd ${RUN_DIR}
cat ${RUN_TYPE}-scale-benchmark.properties | sed 's|${baseRowCount}|'"${ROW_COUNT}|g" | sed 's|${baseDistrib}|'"${DISTRIB}|g" > scale-benchmark.properties

for i in `seq 1 ${ITERATIONS}`; do
  java -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE} -n "${TEST_PATTERN}"
done

if [ "${RUN_TYPE}" = "nightly" ] || [ "${RUN_TYPE}" = "release" ]; then
  for i in `seq 1 ${TAG_ITERS}`; do
    java -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE} -t "Iterate"
  done
fi

title "-- Getting Docker Logs --"
mkdir -p ${RUN_DIR}/logs
cd ${DEEPHAVEN_DIR};
docker compose logs --no-color > ${RUN_DIR}/logs/docker.log &
sleep 10
docker compose down

