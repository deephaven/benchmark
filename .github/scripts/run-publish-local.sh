#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Run queries that publish a secret slack channel. Queries operation exclusively
# the deephaven-benchmark GCloud bucket
if [[ $# != 3 ]]; then
  echo "$0: Missing run type or slack-channel or slack-uri arguments"
  exit 1
fi

CWD=`pwd`
RUN_DIR=${CWD}/publish
GIT_DIR=${CWD}
DEEPHAVEN_DIR=${CWD}
RUN_TYPE=$1
SLACK_CHANNEL=$2
SLACK_URL=$3
BENCH_PROPS_NAME=${RUN_TYPE}-scale-benchmark.properties
BENCH_PROPS_PATH=${GIT_DIR}/.github/resources/${BENCH_PROPS_NAME}

mkdir -p ${RUN_DIR}
cp ${GIT_DIR}/target/deephaven-benchmark-*.jar ${RUN_DIR}/
rm -f ${RUN_DIR}/deephaven-benchmark*-tests.jar
cat ${BENCH_PROPS_PATH} | sed 's|${slackUrl}|'"${SLACK_URL}|g" | sed 's|${slackChannel}'"|${SLACK_CHANNEL}|g" > ${RUN_DIR}/${BENCH_PROPS_NAME}

cd ${DEEPHAVEN_DIR}
sudo docker compose down
sudo docker compose up -d
sleep 10

cd ${RUN_DIR}
java -Dbenchmark.profile=${BENCH_PROPS_NAME} -jar deephaven-benchmark-*.jar publish

cd ${DEEPHAVEN_DIR};
sudo docker compose down
sleep 10

