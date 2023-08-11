#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Run benchmarks on the remote side
# Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 1 ]]; then
  echo "$0: Missing run type argument"
  exit 1
fi

RUN_TYPE=$1
HOST=`hostname`
RUN_DIR=/root/run
DEEPHAVEN_DIR=/root/deephaven

# Match run type (nightly, release, compare) to benchmark test package
case ${RUN_TYPE} in
  compare)
    TEST_PACKAGE=io.deephaven.benchmark.tests.compare
    ;;
  *)
    TEST_PACKAGE=io.deephaven.benchmark.tests.standard
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

cd ${RUN_DIR}
java -Dbenchmark.profile=${RUN_TYPE}-scale-benchmark.properties -jar deephaven-benchmark-*.jar -cp standard-tests.jar -p ${TEST_PACKAGE}

title "-- Getting Docker Logs --"
mkdir -p ${RUN_DIR}/logs
cd ${DEEPHAVEN_DIR};
docker compose logs --no-color > ${RUN_DIR}/logs/docker.log &
sleep 10
docker compose down

