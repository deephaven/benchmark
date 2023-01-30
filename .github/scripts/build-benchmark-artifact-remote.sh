#!/bin/bash

# Build benchmark artifact on the remote side
# Assumes git branch is available and docker is running

HOST=`hostname`
GIT_DIR=/root/git/benchmark
RUN_DIR=/root/run

if [ ! -d "${GIT_DIR}" ] || [ ! -d "${RUN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

echo "- Building Remote Benchmark Artifact on ${HOST} -"

echo "-- Building and Verifying --"
cd ${GIT_DIR}
mvn verify

echo "-- Copying Artifact and Tests to Run Directory --"
mkdir -p ${RUN_DIR}/
cp ${GIT_DIR}/target/deephaven-benchmark-*.jar ${RUN_DIR}/
mv ${RUN_DIR}/deephaven-benchmark-*-tests.jar standard-tests.jar
cp ${GIT_DIR}/.github/resources/*.properties ${RUN_DIR}/