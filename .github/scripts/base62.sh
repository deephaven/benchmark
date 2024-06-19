#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Convert the given number to base62 characters
# ex. base62.sh 1718738365297350992 
# ex. base62.sh $(date +%s%N) 

DECNUM=$1

BASE62=($(echo {0..9} {a..z} {A..Z}))
for i in $(bc <<< "obase=62; ${DECNUM}"); do
  echo -n ${BASE62[$(( 10#$i ))]}
done && echo

