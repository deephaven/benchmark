#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
set -f

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Run benchmarks on the remote side doing one iteration according to the following contract:
# - If TAG_NAME is "Any", run all tests
# - If TAG_NAME starts with "!", run all tests except the named tag
# - Otherwise, run tests marked with the tag name
# Note: Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 6 ]]; then
  echo "$0: Missing run type, test package, test classes, row count, distribution, or tag name"
  echo "run-benchmark-remote $# arguments"
  for i in "$@"; do
    printf '[%s]\n' "$i"
  done
  exit 1
fi

echo "run-benchmark-remote $# arguments"
for i in "$@"; do
  printf '[%s]\n' "$i"
done

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
JAVA_OPTS=$(echo "-Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*-standalone.jar -cp standard-tests.jar")

if [ "${TAG_NAME}" = "Any" ]; then
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -n "${TEST_PATTERN}"
elif [[ "${TAG_NAME}" = !* ]]; then
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -n "${TEST_PATTERN}" -T "${TAG_NAME:1}"
else
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -t "${TAG_NAME}"
fi

title "-- Getting Docker Logs --"
mkdir -p ${RUN_DIR}/logs
cd ${DEEPHAVEN_DIR};
docker compose logs --no-color > ${RUN_DIR}/logs/docker.log &
sleep 10
docker compose down

