#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Generate release notes for the given tags/commits and make a release-notes.md
# file in the working directory

if [[ $# != 3 ]]; then
    echo "$0: Missing release version, release commit or previous version argument"
    exit 1
fi

RELEASE_VERSION=$1
RELEASE_COMMIT=$2
PREVIOUS_VERSION=$3
RELEASE_TAG="v${RELEASE_VERSION}"
PREVIOUS_TAG="v${PREVIOUS_VERSION}"
RELEASE_NOTES=release-notes.md

# A numeric x.y or x.y.z input names a release, which is tagged with a v prefix. Anything else is
# taken as a raw git ref, which is what lets a commit hash or branch be passed instead of a version.
# Detecting the version form explicitly matters now that releases are two-component: the old test
# asked whether the input lacked two dots, which treated 41.0 as a ref and only worked for 0.x.
PREVIOUS_REF=${PREVIOUS_VERSION}
if [[ ${PREVIOUS_VERSION} =~ ^[0-9]+(\.[0-9]+){1,2}$ ]]; then
  PREVIOUS_REF=${PREVIOUS_TAG}
fi

# Make the Release Notes File
echo "**What's Changed**" > ${RELEASE_NOTES}
git log --oneline ${PREVIOUS_REF}...${RELEASE_COMMIT} | sed -e 's/^/- /' >> ${RELEASE_NOTES}
echo "**Full Changelog**: https://github.com/deephaven/benchmark/compare/${PREVIOUS_REF}...${RELEASE_TAG}" >> ${RELEASE_NOTES}

