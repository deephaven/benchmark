#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Run benchmarks on the remote side
# Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 4 ]]; then
  echo "$0: Missing run type, test package, test regex, or row count argument"
  exit 1
fi

RUN_TYPE=$1
TEST_PKG=$2
TEST_RGX=$3
ROW_COUNT=$4
HOST=`hostname`
RUN_DIR=/root/run
DEEPHAVEN_DIR=/root/deephaven

# Match run type (nightly, release, compare, adhoc) to benchmark test package
# Note: Default TEST_PATTERN taken from default -n option in Junit Platform Console Standalone
case ${RUN_TYPE} in
  adhoc)
    TEST_PACKAGE=${TEST_PKG}
    TEST_PATTERN=${TEST_RGX}
    ;;
  compare)
    TEST_PACKAGE=io.deephaven.benchmark.tests.compare
    TEST_PATTERN="^(Test.*|.+[.$]Test.*|.*Tests?)$"
    ;;
  *)
    TEST_PACKAGE=io.deephaven.benchmark.tests.standard
    TEST_PATTERN="^(Test.*|.+[.$]Test.*|.*Tests?)$"
    ;;
esac

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
cat ${RUN_TYPE}-scale-benchmark.properties | sed 's|${baseRowCount}|'"${ROW_COUNT}|g" > scale-benchmark.properties
java -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE} -n "${TEST_PATTERN}"

title "-- Getting Docker Logs --"
mkdir -p ${RUN_DIR}/logs
cd ${DEEPHAVEN_DIR};
docker compose logs --no-color > ${RUN_DIR}/logs/docker.log &
sleep 10
docker compose down

