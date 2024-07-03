#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Fetches Benchmark results and logs from the remote test server and
# compresses the runs before upload

if [[ $# != 6 ]]; then
  echo "$0: Missing host, user, run type, script dir, actor, or run label arguments"
  exit 1
fi

HOST=$1
USER=$2
SCRIPT_DIR=$3
RUN_TYPE=$4
ACTOR=$5
RUN_LABEL=${6:-$(echo -n "set-"; ${SCRIPT_DIR}/base62.sh $(date +%s%03N))}
RUN_DIR=/root/run

# Get the date for the Set Label, since Github Workflows don't have 'with: ${{github.date}}'
if [ "${RUN_LABEL}" = "<date>" ]; then
  RUN_LABEL=$(date '+%Y-%m-%d')
fi

# Pull results from the benchmark server
scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/logs .
scp -r ${USER}@${HOST}:${RUN_DIR}/*.jar .

# Move the results into the destination directory
DEST_DIR=${RUN_TYPE}/${ACTOR}/${RUN_LABEL}
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
rm -rf ${TMP_SVG_DIR}

# Compress CSV and Test Logs
for runId in `find ${DEST_DIR}/ -name "run-*"`
do
  (cd ${runId}; gzip *.csv)
  (cd ${runId}/test-logs; tar -zcvf test-logs.tgz *; mv test-logs.tgz ../)
  rm -rf ${runId}/test-logs/
done

