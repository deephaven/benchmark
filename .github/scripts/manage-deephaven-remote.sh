#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Start or Stop a Deephaven image based on the given directive and image/branch name
# The directives argument can be start or stop
# The supplied image argument can be an image name or <owner>::<branch>

HOST=`hostname`
DEEPHAVEN_DIR=/root/deephaven
DIRECTIVE=$1
DOCKER_IMG=$2
BRANCH_DELIM="::"

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

if [[ $# != 2 ]]; then
  echo "$0: Missing docker directive or image/branch argument"
  exit 1
fi

title () { echo; echo $1; }

title "- Setting up Remote Docker Image on ${HOST} -"

cd ${DEEPHAVEN_DIR}

if [[ ${DOCKER_IMG} != *"${BRANCH_DELIM}"* ]]; then
  echo "DOCKER_IMG=ghcr.io/deephaven/server:${DOCKER_IMG}" > .env
  docker compose pull
else 
  echo "DOCKER_IMG=deephaven/server:benchmark-local" > .env
fi

if [[ ${DIRECTIVE} == 'start' ]]; then
  docker compose up -d
fi

if [[ ${DIRECTIVE} == 'stop' ]]; then
  docker compose down
fi

