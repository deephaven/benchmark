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
GIT_DIR=${HOME}/git
GIT_REPO=$1
GIT_BRANCH=$2
RUN_TYPE=$3                     # ex. nightly | release | compare
DOCKER_IMG=$4			# ex. edge | 0.32.0 (assumes location ghcr.io/deephaven/server)
DEEPHAVEN_DIR=${HOME}/deephaven
export DEBIAN_FRONTEND=noninteractive

title () { echo; echo $1; }

title "- Setting Up Remote Benchmark Testing on ${HOST} -"

title "-- Waiting for APT to be Free --"
BEGIN_SECS=$(date +%s)
STATUS=0
for i in {1..60}; do
  if ! sudo fuser /var/lib/dpkg/lock >/dev/null 2>&1 && ! sudo fuser /var/lib/apt/lists/lock >/dev/null 2>&1 \
      && ! sudo fuser /var/lib/apt/lists/lock-frontend >/dev/null 2>&1; then
    STATUS=1
    break
  fi
  sleep 10
done

DURATION=$(($(date +%s) - ${BEGIN_SECS}))
if [[ $STATUS -eq 0 ]]; then
  echo "Failed to gain APT lock after ${DURATION} seconds"
  exit 1
fi

title "-- Disabling Automatic Updates --"
sudo systemctl stop unattended-upgrades.service 2>/dev/null || true
sudo systemctl stop apt-daily.service 2>/dev/null || true
sudo systemctl stop apt-daily-upgrade.service 2>/dev/null || true
sudo systemctl disable --now apt-daily.timer 2>/dev/null || true
sudo systemctl disable --now apt-daily-upgrade.timer 2>/dev/null || true
sudo systemctl mask unattended-upgrades.service 2>/dev/null || true
sudo systemctl mask apt-daily.service 2>/dev/null || true
sudo systemctl mask apt-daily-upgrade.service 2>/dev/null || true
sudo tee /etc/apt/apt.conf.d/10periodic >/dev/null <<EOF
APT::Periodic::Enable "0";
EOF
sudo tee /etc/apt/apt.conf.d/20auto-upgrades >/dev/null <<EOF
APT::Periodic::Update-Package-Lists "0";
APT::Periodic::Unattended-Upgrade "0";
EOF

title "-- Disabling ASLR for Current Session --"
sudo sysctl -w kernel.randomize_va_space=0 >/dev/null

title "-- Setting Governor Mode --"
echo 1 | sudo tee /sys/devices/system/cpu/cpufreq/boost
sudo cpupower frequency-set -g schedutil

title "-- Disabling SSH Password Authentication --"
sudo sed -i 's/^#\?PasswordAuthentication.*/PasswordAuthentication no/' /etc/ssh/sshd_config
sudo sed -i 's/^#\?KbdInteractiveAuthentication.*/KbdInteractiveAuthentication no/' /etc/ssh/sshd_config
sudo sed -i 's/^#\?ChallengeResponseAuthentication.*/ChallengeResponseAuthentication no/' /etc/ssh/sshd_config
sudo systemctl reload ssh.service

title "-- Adding OS Applications --"
if ! sudo update-alternatives --list java 2>/dev/null | grep -qi temurin; then
  title "-- Adding Adoptium to APT registry --"
  sudo apt-get -y install wget apt-transport-https gpg
  sudo wget -qO - https://packages.adoptium.net/artifactory/api/gpg/key/public | sudo gpg --dearmor | sudo tee /etc/apt/trusted.gpg.d/adoptium.gpg >/dev/null
  echo "deb https://packages.adoptium.net/artifactory/deb $(awk -F= '/^VERSION_CODENAME/{print$2}' /etc/os-release) main" | sudo tee /etc/apt/sources.list.d/adoptium.list
  sudo apt-get -y update
fi

title "-- Installing JVMs --"
sudo apt-get -y install temurin-17-jdk

title "-- Installing Maven --"
sudo apt-get -y install maven

title "-- Installing Docker --"
command_exists() {
  command -v "$@" > /dev/null 2>&1
}
if command_exists docker; then
  echo "Docker already installed... skipping"
else
  sudo apt-get -y update
  sudo apt-get -y install ca-certificates curl
  sudo install -m 0755 -d /etc/apt/keyrings
  sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
  sudo chmod a+r /etc/apt/keyrings/docker.asc

  echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] https://download.docker.com/linux/ubuntu \
    $(. /etc/os-release && echo "$VERSION_CODENAME") stable" | sudo tee /etc/apt/sources.list.d/docker.list > /dev/null
  sudo apt-get -y update
  sudo apt-get -y install docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
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
sudo docker ps -q | xargs --no-run-if-empty -n 1 sudo docker kill

title "-- Removing Docker Containers --"
sudo docker ps -a -q | xargs --no-run-if-empty -n 1 sudo docker rm --force

title "-- Removing Docker Images --"
sudo docker images -a -q | xargs --no-run-if-empty -n 1 sudo docker rmi --force

title "-- Pruning Docker Volumes --"
sudo docker system prune --volumes --force
sudo rm -rf ${DEEPHAVEN_DIR}

title "-- Staging Docker Resources --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/${RUN_TYPE}-benchmark-docker-compose.yml docker-compose.yml



