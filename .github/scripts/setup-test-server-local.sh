# Runs the local (github) side of test server setup
HOST=$1
USER=$2
PRIVATE_KEY=$3
PRIVATE_FILE=~/.ssh/id_workflow

mkdir -p ~/.ssh/
echo "${PRIVATE_KEY}" > ${PRIVATE_FILE}
sudo chmod 600 ${PRIVATE_FILE}
ssh -i ${PRIVATE_FILE} ${USER}@${HOST} 'bash -s' < ./setup-test-server-remote.sh