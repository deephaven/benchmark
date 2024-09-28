#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Build and start the python native Deephaven Community Server

MY_ID=DH_JETTY_BY_STAN
ID_PATTERN="-DMY_ID=${MY_ID}"
JAVA=temurin-17-jdk-amd64
ROOT=./tmp/dh-server-jetty
VENV=${ROOT}/py-venv
BIN=${ROOT}/bin
LOGS=${ROOT}/logs

rm -rf server/jetty-app/build/distributions
rm -rf ${ROOT}
mkdir -p ${BIN} ${LOGS}

OLD_JAVA_HOME="${JAVA_HOME}"
export JAVA_HOME=/usr/lib/jvm/${JAVA}
./gradlew server-jetty-app:build
tar xvf server/jetty-app/build/distributions/server-jetty-*.tar -C ${BIN}
export JAVA_HOME="${OLD_JAVA_HOME}"

rm -f $(find py/server/build/wheel -name "*py3-none-any.whl")
./gradlew py-server:assemble
rm -rf ${VENV}
python3 -m venv ${VENV}
source ${VENV}/bin/activate
pip install "$(find py/server/build/wheel -name '*py3-none-any.whl')[autocomplete]"

export DEBUG="${DEBUG:-"-agentlib:jdwp=transport=dt_socket,server=y,suspend=${SUSPEND:-"n"},address=*:5005"}"

export CONSOLE_TYPE="-Ddeephaven.console.type=${CONSOLE:-"python"}"
export STORAGE="${STORAGE:-"-Dstorage.path=./data/"}"

export CYCLE_TM="${CYCLE_TM:-1000}"
export BARRAGE="${BARRAGE:-"-Dbarrage.minUpdateInterval=${CYCLE_TM} -DPeriodicUpdateGraph.targetCycleDurationMillis=${CYCLE_TM}"}"

export GC_APP="-Dio.deephaven.app.GcApplication.enabled=true"
export GRPC_APP="-Dio.deephaven.server.grpc_api_app.GrpcApiApplication.enabled=true"
export APPS="${APPS:-"$GRPC_APP $GC_APP"}"

export AUTH="-DAuthHandlers=io.deephaven.auth.AnonymousAuthenticationHandler"
export EXTRA_OPS="-Xmx24g ${ID_PATTERN}"

cat redpanda-standalone/docker-compose.yml | sed 's/redpanda:29/localhost:29/g' | sed 's/redpanda:80/localhost:80/g' > ${ROOT}/redpanda-docker-compose.yml
docker compose -f ${ROOT}/redpanda-docker-compose.yml up -d

JAVA_OPTS="$DEBUG $STORAGE $APPS $CONSOLE_TYPE $BARRAGE $AUTH $EXTRA_OPS" ${BIN}/server-jetty-*/bin/start &> ${LOGS}/dh.log &

