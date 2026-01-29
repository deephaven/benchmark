#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Runs the remote test server setup where the benchmarks will be run

if [[ $# != 4 ]]; then
  echo "$0: Missing repo, branch, run type, or docker image argument"
  exit 1
fi

HOST=`hostname`
GIT_DIR=/${HOME}/git
GIT_REPO=$1
GIT_BRANCH=$2
RUN_TYPE=$3                     # ex. nightly | release | compare
DOCKER_IMG=$4			# ex. edge | 0.32.0 (assumes location ghcr.io/deephaven/server)
DEEPHAVEN_DIR=/${HOME}/deephaven

title () { echo; echo $1; }

title "- Setting Up Remote Benchmark Testing on ${HOST} -"

title "-- Adding OS Applications --"
UPDATED=$(sudo update-alternatives --list java | grep -i temurin; echo $?)
if [[ ${UPDATED} != 0 ]]; then
  title "-- Adding Adoptium to APT registry --"
  sudo apt -y install wget apt-transport-https gpg
  sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
  sudo apt -y update
fi

title "-- Installing JVMs --"
sudo apt -y install temurin-17-jdk

title "-- Installing Maven --"
sudo apt -y install maven

title "-- Installing Docker --"
command_exists() {
  command -v "$@" > /dev/null 2>&1
}
if command_exists docker; then
  echo "Docker already installed... skipping"
else
  sudo apt -y update
  sudo apt -y install ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt -y update
  sudo apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
  sudo groupadd docker
  sudo usermod -aG docker ${USER}
fi

title "-- Removing Git Benchmark Repositories --"
sudo rm -rf ${GIT_DIR}
mkdir -p ${GIT_DIR}

title "-- Clone Git Benchmark Repository ${GIT_REPO} --"
cd ${GIT_DIR}
git clone https://github.com/${GIT_REPO}.git
cd benchmark

title "-- Clone Git Benchmark Branch ${GIT_BRANCH} --"
git checkout ${GIT_BRANCH}

title "-- Stopping Docker Containers --"
docker ps -q | xargs --no-run-if-empty -n 1 docker kill

title "-- Removing Docker Containers --"
docker ps -a -q | xargs --no-run-if-empty -n 1 docker rm --force

title "-- Removing Docker Images --"
docker images -a -q | xargs --no-run-if-empty -n 1 docker rmi --force

title "-- Pruning Docker Volumes --"
docker system prune --volumes --force
sudo rm -rf ${DEEPHAVEN_DIR}

title "-- Staging Docker Resources --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/${RUN_TYPE}-benchmark-docker-compose.yml docker-compose.yml



