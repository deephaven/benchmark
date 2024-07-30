#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Run benchmarks on the remote side
# Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 6 ]]; then
  echo "$0: Missing run type, test package, test regex, row count, distribution, or tag name"
  exit 1
fi

RUN_TYPE=$1
TEST_PACKAGE=$2
TEST_PATTERN="$3"
ROW_COUNT=$4
DISTRIB=$5
TAG_NAME=$6
HOST=$(hostname)
RUN_DIR=/root/run
DEEPHAVEN_DIR=/root/deephaven

if [ ! -d "${RUN_DIR}" ]; then
  echo "$0: Missing the Benchmark run directory"
  exit 1
fi

title () { echo; echo $1; }

title "- Running Remote Benchmark Artifact on ${HOST} -"

cd ${DEEPHAVEN_DIR};

title "-- Running Benchmarks --"
cd ${RUN_DIR}
cat ${RUN_TYPE}-scale-benchmark.properties | sed 's|${baseRowCount}|'"${ROW_COUNT}|g" | sed 's|${baseDistrib}|'"${DISTRIB}|g" > scale-benchmark.properties

if [ "${TAG_NAME}" = "Any" ]; then
  java -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE} -n "${TEST_PATTERN}"
else
  java -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE} -t "${TAG_NAME}"
fi

title "-- Getting Docker Logs --"
mkdir -p ${RUN_DIR}/logs
cd ${DEEPHAVEN_DIR};
docker compose logs --no-color > ${RUN_DIR}/logs/docker.log &
sleep 10
docker compose down

