#!/bin/bash

# Runs the remote server setup and benchmark run from github workflow

HOST=$1
USER=$2
PRIVATE_KEY=$3
SCRIPTS_DIR=$4
PRIVATE_FILE=~/.ssh/id_ed25519

if [[ $# != 4 ]]; then
	echo "$0: Wrong number of arguments"
	exit 1
fi

mkdir -p ~/.ssh/
echo "${PRIVATE_KEY}" > ${PRIVATE_FILE}
sudo chmod 600 ${PRIVATE_FILE}
ssh-keyscan -H ${HOST} > ~/.ssh/known_hosts
ssh ${USER}@${HOST} 'bash -s' < ${SCRIPTS_DIR}/setup-test-server-remote.sh
ssh ${USER}@${HOST} 'bash -s' < ${SCRIPTS_DIR}/build-benchmark-artifact-remote.sh
ssh ${USER}@${HOST} 'bash -s' < ${SCRIPTS_DIR}/run-benchmarks-remote.sh
