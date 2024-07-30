#!/usr/bin/env bash

set -o errexit
set -o pipefail

# For a given descriptor, return the appropriate matrix array
# that can be read in a Github workflow with fromJSON(array) that 
# corresponds 
# ex. matrix_array.sh adhoc 5
# ex. matrix_array.sh release 4

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

