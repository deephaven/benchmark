#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Convert the given Base 10 number to Base 62 characters
# ex. base62.sh 1718738365297350992 
# ex. ./base62.sh $(date +%s%03N) 

DECNUM=$1
BASE62=($(echo {0..9} {A..Z} {a..z}))

for i in $(bc <<< "obase=62; ${DECNUM}"); do
  echo -n ${BASE62[$(( 10#$i ))]}
done && echo

