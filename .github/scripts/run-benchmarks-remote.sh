#!/bin/bash

# Run benchmarks on the remote side
# Assumes the deephaven-benchmark-*.jar artifact has been built and placed

HOST=`hostname`
RUN_DIR=/root/run

if [ ! -d "${RUN_DIR}" ]; then
  echo "$0: Missing the Benchmark run directory"
  exit 1
fi

echo "- Running Remote Benchmark Artifact on ${HOST} -"
cd ${RUN_DIR}
java -Dbenchmark.profile=benchmark-10m-colocated.properties -jar deephaven-benchmark-*.jar