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
if [[ %{UPDATED} != 0 ]]; then
  apt install -y wget apt-transport-https gpg
  wget -O - https://packages.adoptium.net/artifactory/api/gpg/key/public | apt-key add -
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | tee /etc/apt/sources.list.d/adoptium.list
  apt update
fi

title "-- Installing JVMs --"
apt install temurin-11-jdk
apt install temurin-21-jdk
# Look at installed packages:  dpkg --list | grep jdk
# Configure default java:  update-alternatives --config java

title "-- Installing Maven --"
apt install maven

title "-- Installing Docker --"
command_exists() {
  command -v "$@" > /dev/null 2>&1
}
if command_exists docker; then
  echo "Docker already installed... skipping"
else
  apt-get install ca-certificates curl gnupg
  install -m 0755 -d /etc/apt/keyringsA
  rm -f /etc/apt/keyrings/docker.gpg
  curl -fsSL https://download.docker.com/linux/ubuntu/gpg | gpg --dearmor -o /etc/apt/keyrings/docker.gpg
  chmod a+r /etc/apt/keyrings/docker.gpg
  echo \
    "deb [arch="$(dpkg --print-architecture)" signed-by=/etc/apt/keyrings/docker.gpg] https://download.docker.com/linux/ubuntu \
    "$(. /etc/os-release && echo "$VERSION_CODENAME")" stable" | \
    tee /etc/apt/sources.list.d/docker.list > /dev/null
    apt-get --assume-yes install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
fi

title "-- Removing Git Benchmark Repositories --"
rm -rf ${GIT_DIR}

title "-- Clone Git Benchmark Repository ${GIT_REPO} --"
mkdir -p ${GIT_DIR}
cd ${GIT_DIR}
git clone https://github.com/${GIT_REPO}.git
cd benchmark

title "-- Clone Git Benchmark Branch ${GIT_BRANCH} --"
git checkout ${GIT_BRANCH}

title "-- Stopping and Removing Docker Installations --"
docker ps -a -q | xargs --no-run-if-empty -n 1 docker stop
docker ps -a -q | xargs --no-run-if-empty -n 1 docker rm
docker images -a -q | xargs --no-run-if-empty -n 1 docker rmi
docker system prune --volumes --force
rm -rf ${DEEPHAVEN_DIR}

title "-- Staging Docker Resources --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/${RUN_TYPE}-benchmark-docker-compose.yml docker-compose.yml



