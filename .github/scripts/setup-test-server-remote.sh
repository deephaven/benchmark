# Runs the remote side of test server setup
HOST=`hostname`
GIT_DIR=/root/git
GIT_REPO=stanbrub
GIT_BRANCH=remote-test-server-workflow
DEEPHAVEN_DIR=/root/deephaven

echo "- Setting Up Remote Benchmark Testing on ${HOST} -"

echo "-- Adding OS Applications --"
apt update

echo "-- Installing Maven --"
apt install maven

echo "-- Installing JDK 17 --"
apt install openjdk-17-jre-headless

echo "-- Installing Docker --"
snap install docker

echo "-- Removing Git Benchmark Project --"
rm -rf ${GIT_DIR}

echo "-- Getting Git Benchmark Project --"
mkdir -p ${GIT_DIR}
cd ${GIT_DIR}
git clone git@github.com:${GIT_REPO}/benchmark.git
cd benchmark
git checkout ${GIT_BRANCH}

echo "-- Stopping and Removing Docker Installations --"
cd ${DEEPHAVEN_DIR}
docker stop $(docker ps -a -q)
docker system prune -f
rm -rf ${DEEPHAVEN_DIR}

echo "-- Installing Deephaven and Redpanda --"
mkdir -p ${DEEPHAVEN_DIR}
cd ${DEEPHAVEN_DIR}
cp ${GIT_DIR}/benchmark/.github/resources/benchmark-docker-compose.yml docker-compose.yml
docker-compose pull

echo "-- Starting Deephaven and Redpanda --"
docker-compose up -d