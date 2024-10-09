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

PREVIOUS_REF=${PREVIOUS_TAG}
if [[ ${PREVIOUS_VERSION} != *"."*"."* ]]; then
  PREVIOUS_REF=${PREVIOUS_VERSION}
fi

# Make the Release Notes File
echo "**What's Changed**" > ${RELEASE_NOTES}
git log --oneline ${PREVIOUS_REF}...${RELEASE_COMMIT} | sed -e 's/^/- /' >> ${RELEASE_NOTES}
echo "**Full Changelog**: https://github.com/deephaven/benchmark/compare/${PREVIOUS_TAG}...${RELEASE_TAG}" >> ${RELEASE_NOTES}

