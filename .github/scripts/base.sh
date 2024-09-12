#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Convert the given Base 10 number to the given radix up to base 62
# ex. base.sh 1718738365297350992 62
# ex. base.sh $(date +%s%03N) 36

DECNUM=$1
RADIX=$2
BASE62=($(echo {0..9} {A..Z} {a..z}))

for i in $(bc <<< "obase=${RADIX}; ${DECNUM}"); do
  echo -n ${BASE62[$(( 10#$i ))]}
done && echo
