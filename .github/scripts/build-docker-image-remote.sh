#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Build a local docker image on the remote side
# Ensure the docker image is running in the Deephaven directory

HOST=`hostname`
GIT_DIR=/root/git
DEEPHAVEN_DIR=/root/deephaven
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

echo "DEEPHAVEN_VERSION: ${DEEPHAVEN_VERSION}"
echo "DEEPHAVEN_CORE_WHEEL: ${DEEPHAVEN_CORE_WHEEL}"
docker buildx bake -f server.hcl

