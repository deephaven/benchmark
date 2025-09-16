#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2024 Deephaven Data Labs and Patent Pending

# Provides what is needed to set up an adhoc benchmark run, including bare metal and labels
# ex. adhoc.sh make-labels "where" "0.36.0" "user123:branch-name-123"
# ex. adhoc.sh metal-deploy api-key project-id c3.small.x86 server-name "2 days" 
# ex. adhoc.sh metal-delete api-key device-id service-name

if [[ $# < 2 ]]; then
  echo "$0: Missing action or its arguments"
  exit 1
fi

ACTION=$1
SCRIPT_DIR=$(dirname "$0")
OUTPUT_NAME=adhoc-${ACTION}.out

rm -f ${OUTPUT_NAME}; touch ${OUTPUT_NAME}

# Get metal device info including ip address
getDeviceInfo() {
  curl --no-progress-meter --max-time 10 -X GET -H "X-Auth-Token: $1" \
    "https://api.equinix.com/metal/v1/devices/$2?include=ip_addresses,state&exclude=root_password,ssh_keys" \
    | jq | tee get-device-response.json | jq -r "$3"
}

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

# Format some number used for scaling the tests
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

# Deploy a bare metal server using the Equinix ReST API
if [[ ${ACTION} == "deploy-metal" ]]; then
  API_KEY=$2
  PROJECT_ID=$3
  PLAN=$4
  ACTOR=$(echo "adhoc-$5-"$(${SCRIPT_DIR}/base.sh $(date +%s%03N) 36) | tr '[:upper:]' '[:lower:]')
  EXPIRE_WHEN=$6
  echo "Deploying Server: ${ACTOR}"

  BEGIN_SECS=$(date +%s)
  DEVICE_ID=$(curl --fail-with-body -X POST \
    -H "Content-Type: application/json" -H "X-Auth-Token: ${API_KEY}" \
    "https://api.equinix.com/metal/v1/projects/${PROJECT_ID}/devices?exclude=plan,ssh_keys,provisioning_events,network_ports,operating_system" \
    -d '{
      "metro": "sv",
      "plan": "'${PLAN}'",
      "operating_system": "ubuntu_22_04",
      "hostname": "'${ACTOR}'",
      "termination_time": "'$(date --iso-8601=seconds -d "+${EXPIRE_WHEN}")'"
    }' | jq | tee create-device-response.json | jq -r '.id')

  IP_ADDRESS="null"
  for i in $(seq 100); do
    echo -n "$i) Device Status: "
    STATE=$(getDeviceInfo ${API_KEY} ${DEVICE_ID} ".state")
    echo "${STATE}"
    if [[ "${STATE}" == "active" ]]; then break; fi
    sleep 6
  done

  DURATION=$(($(date +%s) - ${BEGIN_SECS}))

  IP_ADDRESS=$(getDeviceInfo ${API_KEY} ${DEVICE_ID} ".ip_addresses[0].address")
  STATE=$(getDeviceInfo ${API_KEY} ${DEVICE_ID} ".state")
  if [[ "${IP_ADDRESS}" == "null" ]] || [[ "${STATE}" != "active" ]]; then
    echo "Failed to provision device after ${DURATION} seconds"
    exit 1
  fi

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
  echo "PROVISION_SECS=${DURATION}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_NAME=${ACTOR}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ID=${DEVICE_ID}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ADDR=${IP_ADDRESS}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_EXPIRE=${EXPIRE_WHEN}" | tee -a ${OUTPUT_NAME}
fi

# Delete a bare metal server using the Equlinix ReST API
if [[ ${ACTION} == "delete-metal" ]]; then
  API_KEY=$2
  DEVICE_ID=$3
  DEVICE_NAME=$4

  curl --no-progress-meter --max-time 10 --fail-with-body -X DELETE -H "X-Auth-Token: ${API_KEY}" \
    "https://api.equinix.com/metal/v1/devices/${DEVICE_ID}" \
    | jq | tee delete-device-response.json 

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_NAME=${DEVICE_NAME}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ID=${DEVICE_ID}" | tee -a ${OUTPUT_NAME}
fi



