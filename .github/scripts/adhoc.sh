#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Provides what is needed to set up an adhoc benchmark run, including bare metal and labels
# ex. adhoc.sh make-labels "where" "0.36.0" "user123:branch-name-123"
# ex. adhoc.sh scale-nums 10 5
# ex. adhoc.sh deploy-metal api-key project-id s2.c2.small server-name
# ex. adhoc.sh delete-metal api-key device-id service-name
# ex. adhoc.sh purge-metal api-key project-id

if [[ $# < 2 ]]; then
  echo "$0: Missing action or its arguments"
  exit 1
fi

ACTION=$1
SCRIPT_DIR=$(dirname "$0")
OUTPUT_NAME=adhoc-${ACTION}.out

rm -f ${OUTPUT_NAME}; touch ${OUTPUT_NAME}

# Get the label part of an image/branch name
# ex. edge@sha256:15ab331629805076cdf5ed6666186c6b578298ab493a980779338d153214640e
# ex. user123:1111-my-pull-request
# ex. 0.36.0 or edge
getSetLabel() {
  SUFFIX=$2
  if [[ $2 == *"@sha"*":"* ]]; then
    SUFFIX=$(echo "$2" | sed 's/@sha.*:/_/g' | head -c 20)
  elif [[ $2 == *":"* ]]; then
    SUFFIX=$(echo "$2" | sed 's/.*://g' | head -c 20)
  fi
  echo "${PREFIX}_${SUFFIX}" | sed -E 's/(^[0-9])/_\1/g' | sed 's/[^0-9a-zA-Z_]/_/g'
}

# Make set labels from a prefix and image/branch names
if [[ ${ACTION} == "make-labels" ]]; then
  PREFIX=$2
  IMAGE1=$3
  IMAGE2=$4
  echo "Making Labels: ${PREFIX}"

  LABEL1=$(getSetLabel ${PREFIX} ${IMAGE1})
  LABEL2=$(getSetLabel ${PREFIX} ${IMAGE2})

  echo "PREFIX=${PREFIX}" | tee -a ${OUTPUT_NAME}
  echo "SET_LABEL_1=${LABEL1}" | tee -a ${OUTPUT_NAME}
  echo "SET_LABEL_2=${LABEL2}" | tee -a ${OUTPUT_NAME}
fi

# Format some numbers used for scaling the tests
if [[ ${ACTION} == "scale-nums" ]]; then
  INPUT_ROW_COUNT=$2
  INPUT_ITERATIONS=$3
  echo "Scaling Numbers"

  TEST_ROW_COUNT=$((${INPUT_ROW_COUNT} * 1000000))
  TEST_ITERATIONS=${INPUT_ITERATIONS}
  if [ $((${INPUT_ITERATIONS} % 2)) == 0 ]; then
	TEST_ITERATIONS=$((${INPUT_ITERATIONS} + 1))
  fi

  echo "INPUT_ROW_COUNT=${INPUT_ROW_COUNT}" | tee -a ${OUTPUT_NAME}
  echo "INPUT_ITERATIONS=${INPUT_ITERATIONS}" | tee -a ${OUTPUT_NAME}
  echo "TEST_ROW_COUNT=${TEST_ROW_COUNT}" | tee -a ${OUTPUT_NAME}
  echo "TEST_ITERATIONS=${TEST_ITERATIONS}" | tee -a ${OUTPUT_NAME}
fi

# Deploy a bare metal server using tofu
if [[ ${ACTION} == "deploy-metal" ]]; then
  API_KEY=$2
  PROJECT_ID=$3
  PLAN=$4
  ACTOR=$(echo "adhoc-$5-"$(${SCRIPT_DIR}/base.sh $(date +%s%03N) 36) | tr '[:upper:]' '[:lower:]')
  echo "Deploying Server: ${ACTOR}"
  BEGIN_SECS=$(date +%s)
  
  export TF_VAR_client_id="${PROJECT_ID}"
  export TF_VAR_client_secret="${API_KEY}"
  export TF_VAR_hostname="${ACTOR}"
  export TF_VAR_plan="${PLAN}"
  
  pushd ${SCRIPT_DIR}/../resources
  tofu init -input=false
  tofu apply -auto-approve -input=false
  IP_ADDRESS=$(tofu output -raw public_ip)
  DEVICE_ID=$(tofu output -raw server_id)
  popd

  STATUS=0 
  for i in {1..30}; do
    if ssh -o StrictHostKeyChecking=no benchmark@"${IP_ADDRESS}" "echo ok" 2>/dev/null; then
      STATUS=1
      break
    fi
    sleep 10
  done
  
  DURATION=$(($(date +%s) - ${BEGIN_SECS}))
  if [[ ${STATUS} -eq 0 ]]; then
    echo "Failed to provision device ${ACTOR} after ${DURATION} seconds"
    exit 1
  fi

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
  echo "PROVISION_SECS=${DURATION}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_NAME=${ACTOR}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ID=${DEVICE_ID}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ADDR=${IP_ADDRESS}" | tee -a ${OUTPUT_NAME}
fi

# Delete a bare metal server. Expects that tofu state exists from a previous tofu create
if [[ ${ACTION} == "delete-metal" ]]; then
  API_KEY=$2
  DEVICE_ID=$3
  DEVICE_NAME=$4

  pushd ${SCRIPT_DIR}/../resources
  tofu destroy -auto-approve
  popd

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_NAME=${DEVICE_NAME}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ID=${DEVICE_ID}" | tee -a ${OUTPUT_NAME}
fi

# Purge all ephemeral metal that's past its expiration date
if [[ ${ACTION} == "purge-metal" ]]; then
  API_KEY=$2
  PROJECT_ID=$3
  EXPIRATION_HOURS=24
  
  echo "Starting Ephemeral Server Cleanup"
  echo "Max Hours to Expiration: ${EXPIRATION_HOURS}"
  echo "Requesting OAuth2 Token"
  TOKEN=$(curl -s -X POST -d "grant_type=client_credentials" -d "client_id=${PROJECT_ID}" -d "client_secret=${API_KEY}" \
    https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token | jq -r '.access_token')

  if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
    echo "Failed to obtain OAuth2 Token"
    exit 1
  fi
  echo "OAuth2 Token Acquired"

  CUTOFF=$(date -u -d "$TTL_HOURS hours ago" +"%Y-%m-%dT%H:%M:%SZ")
  echo "Fetching Ephemeral Servers"
  SERVERS=$(curl -s -H "Authorization: Bearer $TOKEN" "https://api.phoenixnap.com/bmc/v1/servers?tag=ephemeral")
  COUNT=$(echo "$servers" | jq 'length')
  echo "Found ${COUNT} Ephemeral Servers."

  echo "${SERVERS}" | jq -c '.[]' | while read server; do
    id=$(echo "$server" | jq -r '.id')
    hostname=$(echo "$server" | jq -r '.hostname')
    created=$(echo "$server" | jq -r '.creationDate')
    if [[ "$created" < "${CUTOFF}" ]]; then
      echo "Deleting Server: $hostname ($id)"
      curl -s -X DELETE -H "Authorization: Bearer $TOKEN" "https://api.phoenixnap.com/bmc/v1/servers/$id" > /dev/null
    fi
  done

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
fi

