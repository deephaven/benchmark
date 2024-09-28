#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Setup SSH for connection between github node and remote host

HOST=$1
PRIVATE_KEY=$2
PRIVATE_FILE=~/.ssh/id_ed25519

if [[ $# != 2 ]]; then
  echo "$0: Missing host or private key arguments"
  exit 1
fi

mkdir -p logs
mkdir -p results
mkdir -p ~/.ssh/
echo "${PRIVATE_KEY}" > ${PRIVATE_FILE}
sudo chmod 600 ${PRIVATE_FILE}
ssh-keyscan -H ${HOST} > ~/.ssh/known_hosts
