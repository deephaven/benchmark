#!/usr/bin/env bash

# set -o errexit
# set -o pipefail
# set -o nounset

# Stop the native python Deephaven Community Server

MY_ID=DH_JETTY_BY_STAN
ID_PATTERN="-DMY_ID=${MY_ID}"
ROOT=./tmp/dh-server-jetty
KILL_FILE=${ROOT}/pkill.stop.count.txt

echo "Shutting Down: ${MY_ID}"
pkill -c -f -SIGTERM "^.*${ID_PATTERN}.*$" > ${KILL_FILE}

STOP_COUNT=$(cat ${KILL_FILE})
if [[ ${STOP_COUNT} -ge 1 ]]; then
  echo "Shut Down: ${MY_ID}"
else
  echo "Shut Down: Nothing to shut down" 
fi

rm -f ${KILL_FILE}

docker compose -f ${ROOT}/redpanda-docker-compose.yml down
