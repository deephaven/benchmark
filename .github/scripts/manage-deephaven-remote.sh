#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Start or Stop a Deephaven image based on the given directive and image/branch name
# The directives argument can be start or stop
# The supplied image argument can be an image name or <owner>::<branch>

if [[ $# -lt 3 ]]; then
  echo "$0: Missing docker directive, image/branch, config options argument"
  exit 1
fi

DIRECTIVE=$1
DOCKER_IMG=$2
CONFIG_OPTS="${@:3}"
HOST=`hostname`
DEEPHAVEN_DIR=/root/deephaven

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

title () { echo; echo $1; }

title "- Setting up Remote Docker Image on ${HOST} -"

cd ${DEEPHAVEN_DIR}

if [[ ${CONFIG_OPTS} == "<default>" ]]; then
  CONFIG_OPTS="-Xmx24g"
fi
echo "CONFIG_OPTS=${CONFIG_OPTS}" > .env

IS_BRANCH="false"
if [[ ${DOCKER_IMG} == *"@sha"*":"* ]]; then
  IS_BRANCH="false"
elif [[ ${DOCKER_IMG} == *":"* ]]; then
  IS_BRANCH="true"
fi

if [[ ${IS_BRANCH} == "false" ]]; then
  echo "DOCKER_IMG=ghcr.io/deephaven/server:${DOCKER_IMG}" >> .env
  docker compose pull
else 
  echo "DOCKER_IMG=deephaven/server:benchmark-local" >> .env
fi

if [[ ${DIRECTIVE} == 'start' ]]; then
  docker compose up -d
fi

if [[ ${DIRECTIVE} == 'stop' ]]; then
  docker compose down
fi

