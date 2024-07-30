#!/usr/bin/env bash

set -o errexit
set -o pipefail

# For a given descriptor, return the appropriate array that can be read by a Github 
# workflow with fromJSON(array)
# ex. matrix_array.sh matrix-arr adhoc 5
# ex. matrix_array.sh matrix-arr release 4

NAME=$1
RUN_TYPE=$2
ITERATIONS=$3

if [ "${RUN_TYPE}" = 'release' ] || [ "${RUN_TYPE}" = 'nightly' ]; then
  FIRST='!Iterate'
  TAG='Iterate'
else
  FIRST='Any'
  TAG='Any'
  ITERATIONS=$((ITERATIONS - 1))
fi

STR='["'${FIRST}'"'

for i in $(seq ${ITERATIONS}); do
  STR=${STR}',"'${TAG}'"'
done

STR=${STR}']'

echo "${NAME}=${STR}"

