#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Executes a local script on a remote server while storing output relative to the 
# local working directory. Also, this script wraps arguments provided for the
# remote scripts in single-quotes to avoid syntax errors.

HOST=$1
USER=$2
SCRIPT_DIR=$3
SCRIPT_NAME=$4

if [[ $# -lt 4 ]]; then
	echo "$0: Missing host, user, script dir, or script name argument"
	exit 1
fi

args=()
for i in ${@:5}; do
  args+=("'"$i"'")
done

ssh -o 'ServerAliveInterval 60' ${USER}@${HOST} 'bash -s' "${args[*]}" < ${SCRIPT_DIR}/${SCRIPT_NAME}.sh |& tee logs/${SCRIPT_NAME}.log
