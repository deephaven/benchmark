#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Fetches Benchmark results and logs from the remote test server and
# compresses the runs before upload. Writes an output file with the 
# SET_LABEL that was used for the set directory name

if [[ $# != 6 ]]; then
  echo "$0: Missing host, user, script dir, run type, actor, or run label arguments"
  exit 1
fi

HOST=$1
USER=$2
SCRIPT_DIR=$3
RUN_TYPE=$4
ACTOR=$5
SET_LABEL=${6:-$(echo -n "set-"; ${SCRIPT_DIR}/base.sh $(date +%s%03N) 62)}
RUN_DIR=/home/${USER}/run
OUTPUT_NAME=fetch-results-local.out

rm -f ${OUTPUT_NAME}; touch ${OUTPUT_NAME}

# Pull results from the benchmark server (Before labelling, since <version> reads the platform csv)
scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/*.jar .

# Get the date for the Set Label, since Github Workflows don't have 'with: ${{github.date}}'
if [ "${SET_LABEL}" = "<date>" ]; then
  SET_LABEL=$(date '+%Y-%m-%d')
fi

# Get the Set Label from the engine-reported version (edge has none), 99 in last place for snapshots
if [ "${SET_LABEL}" = "<version>" ]; then
  PLATFORM_CSV=results/platform-summary-results.csv
  # Match the engine only. The test-runner reports Unknown unless run from a release jar
  vers=$(awk -F, '$2=="deephaven-engine" && $3=="deephaven.version" {print $4; exit}' ${PLATFORM_CSV})
  # Empty would pad to a valid-looking 00.000.00, so fail instead
  : "${vers:?no deephaven-engine deephaven.version found in ${PLATFORM_CSV}}"
  base=${vers%-SNAPSHOT}
  major=$(printf '%02d\n' $(echo ${base} | cut -d "." -f 1))
  minor=$(printf '%03d\n' $(echo ${base} | cut -d "." -f 2))
  if [ "${base}" = "${vers}" ]; then
    patch=$(printf '%02d\n' $(echo ${base} | cut -d "." -f 3))
  else
    patch=99
  fi
  SET_LABEL="${major}.${minor}.${patch}"
  echo "Engine reported ${vers}, using set label ${SET_LABEL}"
fi
echo "SET_LABEL=${SET_LABEL}" | tee -a ${OUTPUT_NAME}

# Move the results into the destination directory
DEST_DIR=${RUN_TYPE}/${ACTOR}/${SET_LABEL}
mkdir -p ${DEST_DIR}
rm -rf ${DEST_DIR}
mv results/ ${DEST_DIR}/

# For now remove any unwanted summaries before uploading to GCloud
rm -f ${DEST_DIR}/*.csv

# Rename the svg summary table according to run type. Discard the rest
TMP_SVG_DIR=${DEST_DIR}/tmp-svg
mkdir -p ${TMP_SVG_DIR}
mv ${DEST_DIR}/*.svg ${TMP_SVG_DIR}
mv ${TMP_SVG_DIR}/${RUN_TYPE}-benchmark-summary.svg ${DEST_DIR}/benchmark-summary.svg
cp ${DEST_DIR}/benchmark-summary.svg ${DEST_DIR}/../
rm -rf ${TMP_SVG_DIR}

# Compress CSV and Test Logs
for runId in `find ${DEST_DIR}/ -name "run-*"`
do
  (cd ${runId}; gzip *.csv)
  (cd ${runId}/test-logs; tar -zcvf test-logs.tgz *; mv test-logs.tgz ../)
  rm -rf ${runId}/test-logs/
done

