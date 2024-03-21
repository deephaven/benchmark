#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Build a local docker image on the remote side if needed
# Ensure the docker image is running in the Deephaven directory

HOST=`hostname`
GIT_DIR=/root/git
DEEPHAVEN_DIR=/root/deephaven
DOCKER_IMG=$1
BRANCH_DELIM="::"

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

if [[ $# != 1 ]]; then
  echo "$0: Missing docker image/branch argument"
  exit 1
fi

title () { echo; echo $1; }

title "- Setting up Remote Docker Image on ${HOST} -"

if [[ ${DOCKER_IMG} != *"${BRANCH_DELIM}"* ]]; then
  cd ${DEEPHAVEN_DIR}
  echo "DOCKER_IMG=ghcr.io/deephaven/server:${DOCKER_IMG}" > .env
  docker compose pull
  title "-- Starting Deephaven and Redpanda --"
  docker compose up -d
  exit 0
fi

readarray -d "${BRANCH_DELIM}" -t splitarr <<< "${DOCKER_IMG}"
OWNER=${splitarr[0]}
BRANCH_NAME=${splitarr[1]}

title "-- Cloning deephaven-core --"
cd ${GIT_DIR}
rm -rf deephaven-core 
git clone https://github.com/${OWNER}/deephaven-core.git
cd deephaven-core
git checkout ${BRANCH_NAME}

title "-- Cloning deephaven-server-docker --"
cd ${GIT_DIR}
rm -rf deephaven-server-docker
git clone https://github.com/deephaven/deephaven-server-docker.git
cd deephaven-server-docker
git checkout main

title "-- Assembling Python Deephaven Core Server --"
cd ${GIT_DIR}/deephaven-core
OLD_JAVA_HOME="${JAVA_HOME}"
export JAVA_HOME=/usr/lib/jvm/${JAVA}
./gradlew outputVersion server-jetty-app:assemble py-server:assemble
export DEEPHAVEN_VERSION=$(cat build/version)
export JAVA_HOME="${OLD_JAVA_HOME}"

title "-- Building Deephaven Docker Image --"
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

title "-- Starting Deephaven and Redpanda --"
cd ${DEEPHAVEN_DIR}
echo "DOCKER_IMG=deephaven/server:benchmark-local" > .env
docker compose up -d



