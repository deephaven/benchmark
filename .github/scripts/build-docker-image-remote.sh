#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Build a local docker image on the remote side
# Ensure the docker image is running in the Deephaven directory

HOST=`hostname`
GIT_DIR=${HOME}/git
DEEPHAVEN_DIR=${HOME}/deephaven
DEEPHAVEN_VERSION_FILE=${GIT_DIR}/deephaven-core/build/version

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

if [ ! -f "${DEEPHAVEN_VERSION_FILE}" ]; then
  echo "$0: Missing Deephaven version file. Was the project built first?"
  exit 1
fi

title () { echo; echo $1; }

if docker image inspect deephaven/server:benchmark-local &>/dev/null 2>&1; then
  echo "Docker image already built. Skipping."
  exit 0
fi

title "- Setting up Remote Docker Image on ${HOST} -"

title "-- Building Deephaven Docker Image --"
export DEEPHAVEN_VERSION=$(cat ${DEEPHAVEN_VERSION_FILE})
cd ${GIT_DIR}/deephaven-server-docker
cp ${GIT_DIR}/deephaven-core/server/jetty-app/build/distributions/server-jetty-*.tar contexts/server/
cp ${GIT_DIR}/deephaven-core/server/jetty-app/build/distributions/server-jetty-*.tar contexts/server-slim/
cp ${GIT_DIR}/deephaven-core/py/server/build/wheel/deephaven_core-*-py3-none-any.whl contexts/server/

export DEEPHAVEN_SOURCES=custom
export DEEPHAVEN_CORE_WHEEL=$(find . -type f -name "*.whl" | xargs -n 1 basename)
export TAG=benchmark-local

# Let server.hcl label the image with the commit, since local images have no digest to identify them
export GIT_REVISION=$(git -C ${GIT_DIR}/deephaven-core rev-parse HEAD)

echo "DEEPHAVEN_VERSION: ${DEEPHAVEN_VERSION}"
echo "DEEPHAVEN_CORE_WHEEL: ${DEEPHAVEN_CORE_WHEEL}"
echo "GIT_REVISION: ${GIT_REVISION}"

# Pull once so bakes resolve locally instead of from Docker Hub, which intermittently 502s
for image in docker/dockerfile:1.4 busybox:latest ubuntu:24.04 eclipse-temurin:21; do
  if docker image inspect ${image} &>/dev/null; then
    continue
  fi
  for attempt in 1 2 3; do
    if docker pull ${image}; then
      break
    fi
    if [ ${attempt} -eq 3 ]; then
      echo "$0: Failed to pull ${image} after ${attempt} attempts"
      exit 1
    fi
    echo "Pull of ${image} failed (attempt ${attempt}); retrying in 30s"
    sleep 30
  done
done

docker buildx bake -f server.hcl

