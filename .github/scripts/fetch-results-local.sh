#!/bin/bash

# Fetches Benchmark results and logs from the remote test server

HOST=$1
USER=$2
RUN_TYPE=$3
RUN_DIR=/root/run

if [[ $# != 3 ]]; then
	echo "$0: Missing host, user, or run type arguments"
	exit 1
fi

mkdir -p results
scp -r ${USER}@${HOST}:${RUN_DIR}/results .
scp -r ${USER}@${HOST}:${RUN_DIR}/logs .
mv results/ ${RUN_TYPE}/