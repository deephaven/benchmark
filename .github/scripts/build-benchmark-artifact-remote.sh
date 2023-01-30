#!/bin/bash

# Build benchmark artifact on the remote side
# Assumes git branch is available and docker is running

HOST=`hostname`
GIT_DIR=/root/git/benchmark
RUN_DIR=/root/run

echo "- Building Remote Benchmark Artifact on ${HOST} -"

echo "-- Building and Verifying --"
cd ${GIT_DIR}
mvn verify

echo "-- Copying Artifact to Run Directory --"
mkdir -p ${RUN_DIR}/
cp ${GIT_DIR}/target/deephaven-benchmark-*.jar ${RUN_DIR}/
cp ${GIT_DIR}/.github/resources/*.properties ${RUN_DIR}/