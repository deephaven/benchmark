#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Build benchmark artifact on the remote side
# Assumes git branch is available and docker is running

HOST=`hostname`
GIT_DIR=/root/git/benchmark
RUN_DIR=/root/run

if [ ! -d "${GIT_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

title () { echo; echo $1; }

title "- Building Remote Benchmark Artifact on ${HOST} -"

title "-- Building and Verifying --"
cd ${GIT_DIR}
mvn verify

title "-- Copying Artifact and Tests to Run Directory --"
rm -rf ${RUN_DIR}
mkdir -p ${RUN_DIR}/
cp ${GIT_DIR}/target/deephaven-benchmark-*.jar ${RUN_DIR}/
mv ${RUN_DIR}/deephaven-benchmark-*-tests.jar ${RUN_DIR}/standard-tests.jar
cp ${GIT_DIR}/.github/resources/*.properties ${RUN_DIR}/