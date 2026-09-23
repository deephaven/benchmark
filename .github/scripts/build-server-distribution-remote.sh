#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Assemble the Deephaven server artifacts on the remote side if needed
# The supplied argument can be an image name or <owner>::<branch>
# Ensure that the artifacts and Deephaven version are available in standard directories

if [[ $# != 1 ]]; then
  echo "$0: Missing docker image/branch argument"
  exit 1
fi

HOST=`hostname`
GIT_DIR=${HOME}/git
DEEPHAVEN_DIR=${HOME}/deephaven
DEEPHAVEN_TAG_FILE=${GIT_DIR}/benchmark-tag
DOCKER_IMG=$1
BRANCH_DELIM=":"
BUILD_JAVA=temurin-17-jdk-amd64

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

title () { echo; echo $1; }

OWNER=$(sed 's/'"${BRANCH_DELIM}"'.*//g' <<< "${DOCKER_IMG}")
BRANCH_NAME=$(sed 's/.*'"${BRANCH_DELIM}"'//g' <<< "${DOCKER_IMG}")
echo "OWNER: ${OWNER}"
echo "BRANCH: ${BRANCH_NAME}"

# Tag per owner/ref so each ref builds once. Slug is for reading, hash is the key
REF_SLUG=$(echo "${OWNER}-${BRANCH_NAME}" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9._-]/-/g' | cut -c1-80)
REF_HASH=$(echo -n "${OWNER}${BRANCH_DELIM}${BRANCH_NAME}" | sha256sum | cut -c1-8)
DOCKER_TAG=benchmark-${REF_SLUG}-${REF_HASH}
echo "DOCKER TAG: ${DOCKER_TAG}"

# Later steps read the tag from here. Written before any exit below.
echo "${DOCKER_TAG}" > ${DEEPHAVEN_TAG_FILE}

# Check the image, not build/version, so a new ref always rebuilds.
if docker image inspect deephaven/server:${DOCKER_TAG} &>/dev/null 2>&1; then
  echo "Image deephaven/server:${DOCKER_TAG} already present. Skipping assemble."
  exit 0
fi

title "-- Cloning deephaven-core --"
cd ${GIT_DIR}
if [ ! -d "deephaven-core" ]; then
  # Do not use --single-branch here, because it does not allow checkout by commit hash 
  git clone https://github.com/${OWNER}/deephaven-core.git
fi
cd deephaven-core
# The clone may be from an earlier row's owner, so repoint origin and discard that row's tree
git remote set-url origin https://github.com/${OWNER}/deephaven-core.git
git reset --hard
git fetch --prune origin
# Prefer the fetched remote ref, since a local branch of the same name may be another owner's
if git rev-parse --verify --quiet "origin/${BRANCH_NAME}" >/dev/null; then
  git checkout --detach "origin/${BRANCH_NAME}"
else
  git checkout --detach "${BRANCH_NAME}"
fi

title "-- Cloning deephaven-server-docker --"
cd ${GIT_DIR}
if [ ! -d "deephaven-server-docker" ]; then
  git clone -b main --single-branch https://github.com/deephaven/deephaven-server-docker.git
fi

title "-- Assembling Python Deephaven Core Server --"
cd ${GIT_DIR}/deephaven-core
export JAVA_HOME=/usr/lib/jvm/${BUILD_JAVA}

# The image build copies these with globs, so a leftover from another ref could be picked up.
rm -rf server/jetty-app/build/distributions py/server/build/wheel

echo "org.gradle.daemon=false" >> gradle.properties
./gradlew outputVersion server-jetty-app:assemble py-server:assemble

echo "Assembled ${OWNER}${BRANCH_DELIM}${BRANCH_NAME} -> $(git rev-parse HEAD) for tag ${DOCKER_TAG}"



