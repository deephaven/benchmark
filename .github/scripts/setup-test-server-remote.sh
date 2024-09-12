#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Runs the remote side of test server setup

if [ ! -d "/root" ]; then
  echo "$0: Missing the Benchmark install directory"
  exit 1
fi

if [[ $# != 4 ]]; then
  echo "$0: Missing repo, branch, run type, or docker image argument"
  exit 1
fi

HOST=`hostname`
GIT_DIR=/root/git
GIT_REPO=$1
GIT_BRANCH=$2
RUN_TYPE=$3                     # ex. nightly | release | compare
DOCKER_IMG=$4			# ex. edge | 0.32.0 (assumes location ghcr.io/deephaven/server)
DEEPHAVEN_DIR=/root/deephaven

title () { echo; echo $1; }

title "- Setting Up Remote Benchmark Testing on ${HOST} -"

title "-- Adding OS Applications --"
UPDATED=$(update-alternatives --list java | grep -i temurin; echo $?)
if [[ ${UPDATED} != 0 ]]; then
  title "-- Adding Adoptium to APT registry --"
  apt -y install wget apt-transport-https gpg
  wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | gpg --dearmor | tee /etc/apt/trusted.gpg.d/adoptium.gpg > /dev/null
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
  apt -y update
fi

title "-- Installing JVMs --"
apt -y install temurin-11-jdk
apt -y install temurin-21-jdk

title "-- Installing Maven --"
apt -y install maven

title "-- Installing Docker --"
command_exists() {
  command -v "$@" > /dev/null 2>&1
}
if command_exists docker; then
  echo "Docker already installed... skipping"
else
  apt -y update
  apt -y install ca-certificates curl
  install -m 0755 -d /etc/apt/keyrings
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  chmod a+r /etc/apt/keyrings/docker.asc

  echo \
    "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null
  apt -y update
  apt -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

title "-- Removing Git Benchmark Repositories --"
rm -rf ${GIT_DIR}
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
rm -rf ${DEEPHAVEN_DIR}

title "-- Staging Docker Resources --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/${RUN_TYPE}-benchmark-docker-compose.yml docker-compose.yml



