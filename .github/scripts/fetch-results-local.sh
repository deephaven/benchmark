#!/bin/bash

# Fetches Benchmark results and logs from the remote test server

HOST=$1
USER=$2
RUN_DIR=/root/run

if [[ $# != 2 ]]; then
	echo "$0: Missing host or user arguments"
	exit 1
fi

scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/logs .
