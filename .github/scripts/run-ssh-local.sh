#!/bin/bash

# Runs the remote server setup and benchmark run from github workflow

HOST=$1
USER=$2
SCRIPT_DIR=$3
SCRIPT_NAME=$4


if [[ $# -lt 4 ]]; then
	echo "$0: Wrong number of arguments"
	exit 1
fi

ssh ${USER}@${HOST} 'bash -s' "${@:5}" < ${SCRIPT_DIR}/${SCRIPT_NAME}.sh |& tee logs/${SCRIPT_NAME}.log

