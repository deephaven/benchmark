#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset
set -f

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

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

args=()
for i in ${@:5}; do
  args+=("'"$i"'")
done

MAX_RETRIES=5
RETRY_DELAY=15
for ((attempt=1; attempt<=MAX_RETRIES; attempt++)); do
  if ssh -o 'ConnectTimeout 10' ${USER}@${HOST} true; then
    break
  fi
  if [[ $attempt -lt $MAX_RETRIES ]]; then
    echo "SSH connection attempt $attempt failed. Retrying in ${RETRY_DELAY}s..."
    sleep $RETRY_DELAY
  else
    echo "SSH connection failed after $MAX_RETRIES attempts"
    exit 1
  fi
done

# Redact the host from the log file, since artifacts are public and GitHub masks only the live log
HOST_PATTERN=$(printf '%s' "${HOST}" | sed 's/[^a-zA-Z0-9-]/\\&/g')

ssh -o 'ConnectTimeout 10' -o 'ServerAliveInterval 60' ${USER}@${HOST} 'bash -s' -- "${args[@]}" < ${SCRIPT_DIR}/${SCRIPT_NAME}.sh |& sed -u "s/${HOST_PATTERN}/<redacted-host>/g" | tee logs/${SCRIPT_NAME}.log
