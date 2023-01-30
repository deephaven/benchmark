#!/bin/bash

# Setup SSH for connection between github node and remote host

HOST=$1
PRIVATE_KEY=$2
PRIVATE_FILE=~/.ssh/id_ed25519

if [[ $# != 2 ]]; then
	echo "$0: Missing host or private key arguments"
	exit 1
fi

mkdir -p logs
mkdir -p ~/.ssh/
echo "${PRIVATE_KEY}" > ${PRIVATE_FILE}
sudo chmod 600 ${PRIVATE_FILE}
ssh-keyscan -H ${HOST} > ~/.ssh/known_hosts
