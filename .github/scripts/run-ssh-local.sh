#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Executes a local script on a remote server while storing output relative to the 
# local working directory. Also, this script wraps arguments provided for the
# remote scripts in single-quotes to avoid syntax errors.

if [[ $# -lt 4 ]]; then
	echo "$0: Missing host, user, script dir, or script name argument"
	exit 1
fi

HOST=$1
USER=$2
SCRIPT_DIR=$3
SCRIPT_NAME=$4

for ((i = 1; i <= $#; i++)); do
  set -f
  echo "Argument $i: ${!i}"
  set +f
done

args=()
for i in ${@:5}; do
  args+=("'"$i"'")
done

ssh -o 'ServerAliveInterval 60' ${USER}@${HOST} 'bash -s' "${args[*]}" < ${SCRIPT_DIR}/${SCRIPT_NAME}.sh |& tee logs/${SCRIPT_NAME}.log
