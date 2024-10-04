#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Create a tar file with the given tag using the git project located in the 
# working directory 

TAG=$1
ARTIFACT=deephaven-benchmark-${TAG}
DISTRO=target/distro
THIS=$(basename "$0")

mkdir -p ${DISTRO}/libs/
cp .github/distro/* ${DISTRO}
cp target/dependencies/* ${DISTRO}/libs
cp target/deephaven-benchmark-1.0-SNAPSHOT.jar ${DISTRO}/libs/${ARTIFACT}.jar
cp target/deephaven-benchmark-1.0-SNAPSHOT-tests.jar ${DISTRO}/libs/${ARTIFACT}-tests.jar
echo "VERSION=${TAG}" > ${DISTRO}/.env

cd ${DISTRO}
tar cvzf ../${ARTIFACT}.tar * .env

