#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Assemble the Deephaven server artifacts on the remote side if needed
# The supplied argument can be an image name or <owner>::<branch>
# Ensure that the artifacts and Deephaven version are available in standard directories

HOST=`hostname`
GIT_DIR=/root/git
DEEPHAVEN_DIR=/root/deephaven
DOCKER_IMG=$1
BRANCH_DELIM="::"
BUILD_JAVA=temurin-11-jdk-amd64

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

if [[ $# != 1 ]]; then
  echo "$0: Missing docker image/branch argument"
  exit 1
fi

title () { echo; echo $1; }

OWNER=$(sed 's/'"${BRANCH_DELIM}"'.*//g' <<< "${DOCKER_IMG}")
BRANCH_NAME=$(sed 's/.*'"${BRANCH_DELIM}"'//g' <<< "${DOCKER_IMG}")
echo "OWNER: ${OWNER}"
echo "BRANCH: ${BRANCH_NAME}"

title "-- Cloning deephaven-core --"
cd ${GIT_DIR}
rm -rf deephaven-core
git clone -b ${BRANCH_NAME} --single-branch https://github.com/${OWNER}/deephaven-core.git

title "-- Cloning deephaven-server-docker --"
cd ${GIT_DIR}
rm -rf deephaven-server-docker
git clone -b main --single-branch https://github.com/${OWNER}/deephaven-server-docker.git

title "-- Assembling Python Deephaven Core Server --"
cd ${GIT_DIR}/deephaven-core
export JAVA_HOME=/usr/lib/jvm/${BUILD_JAVA}

echo "org.gradle.daemon=false" >> gradle.properties
./gradlew outputVersion server-jetty-app:assemble py-server:assemble


