#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Test the release distro created in the target directory
# - Untar is into a new directory
# - Run a small set of tests with the benchmark.sh script

TAG=$1
ARTIFACT=deephaven-benchmark-${TAG}
DISTRO=target/distro
TEST_DIR=target/test-distro

mkdir -p ${TEST_DIR}
cp target/${ARTIFACT}.tar ${TEST_DIR}
cd ${TEST_DIR}
tar xvf ${ARTIFACT}.tar

./benchmark.sh 1 Where

tar cvzf ../${ARTIFACT}-results.tar results/
