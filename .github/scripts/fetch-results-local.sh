#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Fetches Benchmark results and logs from the remote test server and
# compresses the runs before upload

HOST=$1
USER=$2
RUN_TYPE=$3
RUN_DIR=/root/run

if [[ $# != 3 ]]; then
    echo "$0: Missing host, user, or run type arguments"
    exit 1
fi

# Pull results from the benchmark server
scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/logs .
rm -rf ${RUN_TYPE}
mv results/ ${RUN_TYPE}/

# For now remove any unwanted summaries before uploading to GCloud
rm -f ${RUN_TYPE}/*.csv

# Rename the svg summary table according to run type. Discard the rest
TMP_SVG_DIR=${RUN_TYPE}/tmp-svg
mkdir -p ${TMP_SVG_DIR}
mv ${RUN_TYPE}/*.svg ${TMP_SVG_DIR}
mv ${TMP_SVG_DIR}/${RUN_TYPE}-benchmark-summary.svg ${RUN_TYPE}/benchmark-summary.svg
rm -rf ${TMP_SVG_DIR}

# Compress CSV and Test Logs
for runId in `find ${RUN_TYPE}/ -name "run-*"`
do
    (cd ${runId}; gzip *.csv)
    (cd ${runId}/test-logs; tar -zcvf test-logs.tgz *; mv test-logs.tgz ../)
    rm -rf ${runId}/test-logs/
done
