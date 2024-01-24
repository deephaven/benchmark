#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Fetches Benchmark results and logs from the remote test server and
# compresses the runs before upload

HOST=$1
USER=$2
RUN_TYPE=$3
ACTOR=${4:-}
RUN_LABEL=${5:-}
RUN_DIR=/root/run

if [[ $# != 3 ]] && [[ $# != 5 ]]; then
    echo "$0: Missing host, user, run type, actor, or run label arguments"
    exit 1
fi

# Pull results from the benchmark server
scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/logs .

# If the RUN_TYPE is adhoc, userfy the destination directory
DEST_DIR=${RUN_TYPE}
if [ "${RUN_TYPE}" = "adhoc" ]; then
    if [ -z "${ACTOR}" ] || [ -z "${RUN_LABEL}" ]; then
        echo "$0: Missing actor of run label argument for adhoc run type"
        exit 1
    fi
    DEST_DIR=${RUN_TYPE}/${ACTOR}/${RUN_LABEL}
    mkdir -p ${DEST_DIR}
fi

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

