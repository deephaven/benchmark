#!/bin/bash

# Runs the remote side of test server setup

HOST=`hostname`
GIT_DIR=/root/git
GIT_REPO=stanbrub
GIT_BRANCH=remote-test-server-workflow
DEEPHAVEN_DIR=/root/deephaven

if [ ! -d "/root" ]; then
  echo "$0: Missing the Benchmark install directory"
  exit 1
fi

title () { echo; echo $1; }

title "- Setting Up Remote Benchmark Testing on ${HOST} -"

title "-- Adding OS Applications --"
apt update

title "-- Installing Maven --"
apt install maven

title "-- Installing JDK 17 --"
apt install openjdk-17-jre-headless

title "-- Installing Docker --"
snap install docker

title "-- Removing Git Benchmark Project --"
rm -rf ${GIT_DIR}

title "-- Getting Git Benchmark Project --"
mkdir -p ${GIT_DIR}
cd ${GIT_DIR}
git clone git@github.com:${GIT_REPO}/benchmark.git
cd benchmark
git checkout ${GIT_BRANCH}

title "-- Stopping and Removing Docker Installations --"
cd ${DEEPHAVEN_DIR}
docker stop $(docker ps -a -q)
docker system prune -f
rm -rf ${DEEPHAVEN_DIR}

title "-- Installing Deephaven and Redpanda --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/benchmark-docker-compose.yml docker-compose.yml
docker-compose pull

title "-- Starting Deephaven and Redpanda --"
docker-compose up -d