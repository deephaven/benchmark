#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
set -f

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Run benchmarks on the remote side doing one iteration according to the following contract:
# - If TAG_NAME is "Any", run all tests
# - If TAG_NAME starts with "!", run all tests except the named tag
# - Otherwise, run tests marked with the tag name
# Every form is limited to the classes matching TEST_PATTERN. The name filter (-n) and the tag
# filters (-t/-T) are independent and intersect, so a short run can narrow both at once.
# Note: Assumes the deephaven-benchmark-*.jar artifact has been built and placed

if [[ $# != 6 ]]; then
  echo "$0: Missing run type, test package, test classes, row count, distribution, or tag name"
  exit 1
fi

RUN_TYPE=$1
TEST_PACKAGE=$2
TEST_PATTERN="$3"
ROW_COUNT=$4
DISTRIB=$5
TAG_NAME=$6
HOST=$(hostname)
RUN_DIR=${HOME}/run
DEEPHAVEN_DIR=${HOME}/deephaven

if [ ! -d "${RUN_DIR}" ]; then
  echo "$0: Missing the Benchmark run directory"
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
  if [[ -n ${ref} ]]; then
    digest=$(docker image inspect --format '{{if .RepoDigests}}{{index .RepoDigests 0}}{{end}}' "${ref}" 2>/dev/null) || true
    revision=$(docker image inspect --format '{{index .Config.Labels "org.opencontainers.image.revision"}}' "${ref}" 2>/dev/null) || true
  fi
  if [[ ${digest} == *"@"* ]]; then
    ref=${ref%@*}@${digest#*@}
  fi
  if [[ ${revision} == "<no value>" ]]; then
    revision=""
  fi
  echo "docker.image.digest=${ref}" > ${props}
  echo "docker.image.revision=${revision}" >> ${props}
  cat ${props}
}

title "- Running Remote Benchmark Artifact on ${HOST} -"

cd ${DEEPHAVEN_DIR};

title "-- Recording Deephaven Image Identity --"
write_image_props

# Merge JFR recordings dumped by the engine when -Dbench.jfr is set (see Bench.dumpRecording) from
# data/jfr/<run-id>/<test class> into results/<run-id>/test-logs/<test class>, so they are uploaded in
# test-logs.tgz with the run instead of left on the server
collect_jfr () {
  local jfr_dir run_id test_logs
  for jfr_dir in ${DEEPHAVEN_DIR}/data/jfr/run-*/; do
    run_id=$(basename "${jfr_dir}")
    test_logs=${RUN_DIR}/results/${run_id}/test-logs
    if [[ -d ${jfr_dir} && -d ${test_logs} ]]; then
      sudo cp -r "${jfr_dir}." ${test_logs}/
      sudo chown -R $(id -u):$(id -g) ${test_logs}
      echo "${run_id}: $(find ${test_logs} -name '*.jfr' | wc -l) recordings, $(du -sh ${test_logs} | cut -f1) test-logs"
    fi
  done
  sudo rm -rf ${DEEPHAVEN_DIR}/data/jfr
}

title "-- Clearing JFR Recordings Left by a Failed Run --"
sudo rm -rf ${DEEPHAVEN_DIR}/data/jfr

title "-- Running Benchmarks --"
set +f
cd ${RUN_DIR}
cat ${RUN_TYPE}-scale-benchmark.properties | sed 's|${baseRowCount}|'"${ROW_COUNT}|g" | sed 's|${baseDistrib}|'"${DISTRIB}|g" | sed 's|${userHome}|'"${HOME}|g" > scale-benchmark.properties
JAVA_OPTS=$(echo -Xmx4g -Dbenchmark.profile=scale-benchmark.properties -jar deephaven-benchmark-*-standalone.jar -cp standard-tests.jar)
set -f

if [ "${TAG_NAME}" = "Any" ]; then
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -n "${TEST_PATTERN}"
elif [[ "${TAG_NAME}" = !* ]]; then
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -n "${TEST_PATTERN}" -T "${TAG_NAME:1}"
else
  java ${JAVA_OPTS} -p ${TEST_PACKAGE} -n "${TEST_PATTERN}" -t "${TAG_NAME}"
fi

title "-- Collecting JFR Recordings --"
set +f
collect_jfr
set -f

title "-- Stopping Docker Services --"
cd ${DEEPHAVEN_DIR};
docker compose down

