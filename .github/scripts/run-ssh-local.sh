#!/bin/bash

# Runs the remote server setup and benchmark run from github workflow

HOST=$1
USER=$2
SCRIPT_DIR=$3
SCRIPT_NAME=$4

if [[ $# != 4 ]]; then
	echo "$0: Missing arguments"
	exit 1
fi

ssh ${USER}@${HOST} 'bash -s' < ${SCRIPT_DIR}/${SCRIPT_NAME}.sh |& tee ${SCRIPT_NAME}.log

