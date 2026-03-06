#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2023-2026 Deephaven Data Labs and Patent Pending

# Provides what is needed to set up an adhoc benchmark run, including bare metal and labels
# ex. adhoc.sh make-labels "where" "0.36.0" "user123:branch-name-123"
# ex. adhoc.sh scale-nums 10 5
# ex. adhoc.sh deploy-metal api-key project-id s2.c2.small server-name
# ex. adhoc.sh delete-metal api-key project-id device-id server-name
# ex. adhoc.sh purge-metal api-key project-id

if [[ $# -lt 2 ]]; then
  echo "$0: Missing action or its arguments"
  exit 1
fi

ACTION=$1
SCRIPT_DIR=$(dirname "$0")
OUTPUT_NAME=adhoc-${ACTION}.out
SERVER_NAME_PREFIX="adhoc-"

rm -f ${OUTPUT_NAME}; touch ${OUTPUT_NAME}

# Get the label part of an image/branch name
# ex. edge@sha256:15ab331629805076cdf5ed6666186c6b578298ab493a980779338d153214640e
# ex. user123:1111-my-pull-request
# ex. 0.36.0 or edge
getSetLabel() {
  PREFIX="$1"
  SUFFIX="$2"
  if [[ $2 == *"@sha"*":"* ]]; then
    SUFFIX=$(echo "$2" | sed 's/@sha.*:/_/g' | head -c 20)
  elif [[ $2 == *":"* ]]; then
    SUFFIX=$(echo "$2" | sed 's/.*://g' | head -c 20)
  fi
  echo "${PREFIX}_${SUFFIX}" | sed -E 's/(^[0-9])/_\1/g' | sed 's/[^0-9a-zA-Z_]/_/g'
}

getApiToken() {
  curl -s -X POST "https://auth.phoenixnap.com/auth/realms/BMC/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" --data-urlencode "grant_type=client_credentials" \
    --data-urlencode "client_id=$1" --data-urlencode "client_secret=$2" | jq -r '.access_token'
}

deleteMetal() {
    TOKEN="$1"
    DEVICE_ID="$2"
    DEVICE_NAME="$3"
    echo "Deleting server ${DEVICE_NAME}"
    RESP_CODE=$(curl -s -o /dev/null -w "%{http_code}" -X DELETE -H "Authorization: Bearer ${TOKEN}" "https://api.phoenixnap.com/bmc/v1/servers/${DEVICE_ID}")
    CURL_EXIT=$?
    if (( ${CURL_EXIT} != 0 )); then
        echo "Failed deleting server ${DEVICE_NAME} with exit code ${CURL_EXIT}"
        exit 1
    fi
    if (( ${RESP_CODE} != 404 && (${RESP_CODE} < 200 || ${RESP_CODE} >= 300) )); then
        echo "Failed deleting server ${DEVICE_NAME} with http code ${RESP_CODE}"
        exit 1
    fi
    echo "Successfully deleted server ${DEVICE_NAME}"
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

# Deploy a bare metal server using the Phoenix NAP REST API
if [[ ${ACTION} == "deploy-metal" ]]; then
  API_KEY=$2
  PROJECT_ID=$3
  PLAN=$4
  ACTOR=$(echo "${SERVER_NAME_PREFIX}$5-"$(${SCRIPT_DIR}/base.sh $(date +%s%03N) 36) | tr '[:upper:]' '[:lower:]')
  echo "Deploying Server: ${ACTOR}"
  BEGIN_SECS=$(date +%s)
  
  pushd ${SCRIPT_DIR}/../resources
  TOKEN=$(getApiToken "${PROJECT_ID}" "${API_KEY}")

  echo "Making Deploy POST"
  jq --arg hostname "${ACTOR}" --arg plan "${PLAN}" \
   '.hostname = $hostname | .type = $plan' adhoc-server-deploy.json > adhoc-server-deploy-final.json
  echo "Finished Deploy POST"

  echo "Running Deploy API"
  RESPONSE=$(curl -s -X POST "https://api.phoenixnap.com/bmc/v1/servers" -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: application/json" -d @adhoc-server-deploy-final.json)

  IP_ADDRESS=$(jq -r '.publicIpAddresses[0] // empty' <<< "${RESPONSE}")
  DEVICE_ID=$(jq -r '.id // empty' <<< "${RESPONSE}")
  if [[ -z "$IP_ADDRESS" || -z "$DEVICE_ID" ]]; then
    echo "Failed Getting IP Address and Device ID"
    exit 1
  else
    echo "Got Address ${IP_ADDRESS} and Device Id ${DEVICE_ID}"
  fi
  popd
  
  echo "Waiting for Server Ready"
  STATUS=0
  for i in {1..60}; do
    STATUS_RESPONSE=$(curl -s -H "Authorization: Bearer ${TOKEN}" "https://api.phoenixnap.com/bmc/v1/servers/${DEVICE_ID}")
    SERVER_STATUS=$(echo "${STATUS_RESPONSE}" | jq -r '.status')
    if [[ "${SERVER_STATUS}" == "powered-on" ]]; then
      if nc -z -w 2 "${IP_ADDRESS}" 22 >/dev/null 2>&1; then
        STATUS=1
        break
      fi
    fi
    sleep 10
  done
  
  DURATION=$(($(date +%s) - ${BEGIN_SECS}))
  if [[ ${STATUS} -eq 0 ]]; then
    echo "Failed to provision device ${ACTOR} after ${DURATION} seconds"
    echo "Last provision attempt status: ${SERVER_STATUS}"
    exit 1
  fi

  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
  echo "PROVISION_SECS=${DURATION}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_NAME=${ACTOR}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ID=${DEVICE_ID}" | tee -a ${OUTPUT_NAME}
  echo "DEVICE_ADDR=${IP_ADDRESS}" | tee -a ${OUTPUT_NAME}
fi

# Delete a bare metal server with the given id
if [[ ${ACTION} == "delete-metal" ]]; then
  API_KEY=$2
  PROJECT_ID=$3
  DEVICE_ID=$4
  DEVICE_NAME=$5

  TOKEN=$(getApiToken "${PROJECT_ID}" "${API_KEY}")
  echo "Deleting Server ${DEVICE_NAME}"
  deleteMetal ${TOKEN} ${DEVICE_ID} ${DEVICE_NAME}
  
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
  TOKEN=$(getApiToken "${PROJECT_ID}" "${API_KEY}")
  THRESHOLD=$(( EXPIRATION_HOURS * 3600 ))
  NOW=$(date +%s)

  servers=$(curl -s -H "Authorization: Bearer ${TOKEN}" https://api.phoenixnap.com/bmc/v1/servers)
  echo "$servers" | jq -c '.[]' | while read server; do
    id=$(echo "$server" | jq -r '.id')
    name=$(echo "$server" | jq -r '.hostname')
    created=$(echo "$server" | jq -r '.provisionedOn')
    [ "$created" = "null" ] && continue
    created_epoch=$(date -d "$created" +%s)
    age_seconds=$(( NOW - created_epoch ))
    echo "Found Server $name Aged $(( age_seconds / 3600 )) Hours"

    if [[ "$name" == "${SERVER_NAME_PREFIX}"* ]] && (( age_seconds > THRESHOLD )); then
      deleteMetal ${TOKEN} $id $name
    fi
  done
  echo "ACTION=${ACTION}" | tee -a ${OUTPUT_NAME}
fi

