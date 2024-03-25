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
export JAVA_HOME=/usr/lib/jvm/${BUILD_JAVA}

echo "org.gradle.daemon=false" >> gradle.properties
./gradlew outputVersion server-jetty-app:assemble py-server:assemble


