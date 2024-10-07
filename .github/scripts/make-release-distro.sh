#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Create a tar file with the given version using the git project located in the 
# working directory 

if [[ $# != 3 ]]; then
    echo "$0: Missing release version, release commit or previous version/commit argument"
    exit 1
fi

RELEASE_VERSION=$1
RELEASE_COMMIT=$2
PREVIOUS_VERSION=$3
RELEASE_TAG="v${RELEASE_VERSION}"
PREVIOUS_TAG="v${PREVIOUS_VERSION}"
ARTIFACT=deephaven-benchmark-${RELEASE_VERSION}
DISTRO=target/distro
THIS=$(basename "$0")

# Make the Release Notes File
echo "**What's Changed**" > release-notes.md
git log --oneline ${PREVIOUS_TAG}...${RELEASE_COMMIT} | sed -e 's/^/- /' >> release-notes.md
echo "**Full Changelog**: https://github.com/deephaven/benchmark/compare/${PREVIOUS_TAG}...${RELEASE_TAG}" >> release-notes.md

# Build the Distro for running standard benchmarks
mkdir -p ${DISTRO}/libs/
cp .github/distro/* ${DISTRO}
cp target/dependencies/* ${DISTRO}/libs
cp target/deephaven-benchmark-1.0-SNAPSHOT.jar ${DISTRO}/libs/${ARTIFACT}.jar
cp target/deephaven-benchmark-1.0-SNAPSHOT-tests.jar ${DISTRO}/libs/${ARTIFACT}-tests.jar
echo "VERSION=${RELEASE_VERSION}" > ${DISTRO}/.env

cd ${DISTRO}
tar cvzf ../${ARTIFACT}.tar * .env

