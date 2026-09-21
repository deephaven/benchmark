#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

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
DEEPHAVEN_DIR=${HOME}/deephaven

if [ ! -d "${DEEPHAVEN_DIR}" ]; then
  echo "$0: Missing one or more Benchmark setup directories"
  exit 1
fi

title () { echo; echo $1; }

# Save the image digest and revision where the engine can read them, since it cannot inspect its own image
write_image_props () {
  local props=${DEEPHAVEN_DIR}/data/deephaven-image.properties
  local ref="" digest="" revision=""
  mkdir -p ${DEEPHAVEN_DIR}/data
  rm -f ${props}
  ref=$(docker compose config --images deephaven 2>/dev/null | head -1) || true
  if [[ -z ${ref} ]]; then
    echo "$0: Warning: No deephaven service image configured. Skipping image properties"
    return 0
  fi
  digest=$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "${ref}" 2>/dev/null) || true
  revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "${ref}" 2>/dev/null) || true
  if [[ ${digest} == *"@"* ]]; then
    echo "docker.image.digest=${ref%@*}@${digest#*@}" > ${props}
  else
    echo "docker.image.digest=${ref}" > ${props}
  fi
  if [[ -n ${revision} && ${revision} != "<no value>" ]]; then
    echo "docker.image.revision=${revision}" >> ${props}
  fi
  cat ${props}
}

title "- Setting up Remote Docker Image on ${HOST} -"

cd ${DEEPHAVEN_DIR}

if [[ ${CONFIG_OPTS} == "<default>" ]]; then
  CONFIG_OPTS="-Xmx24g"
fi
echo "CONFIG_OPTS=${CONFIG_OPTS}" > .env
echo "ENV_DEEPHAVEN_HOST_OS_DIR=${DEEPHAVEN_DIR}" >> .env

if [[ ${DOCKER_IMG} == ghcr.io/* ]]; then
  echo "DOCKER_IMG=${DOCKER_IMG}" >> .env
  docker compose pull
elif [[ ${DOCKER_IMG} == *":"* ]]; then
  echo "DOCKER_IMG=deephaven/server:benchmark-local" >> .env
else
  echo "DOCKER_IMG=ghcr.io/deephaven/server:${DOCKER_IMG}" >> .env
  docker compose pull
fi

if [[ ${DIRECTIVE} == 'start' ]]; then
  title "-- Recording Deephaven Image Identity --"
  write_image_props
  docker compose up -d
fi

if [[ ${DIRECTIVE} == 'stop' ]]; then
  docker compose down
fi

