#!/usr/bin/env bash

set -o errexit
set -o pipefail

# Copyright (c) 2026-2026 Deephaven Data Labs and Patent Pending

# Runs a matrix script and converts its TSV output to a JSON matrix for GitHub Actions.
# The matrix script outputs tab-separated rows with fields:
#   run_type, run_label, docker_image, test_package, test_class_list,
#   test_iterations, scale_row_count, distribution, config_options
# Every field is required, so a new field means updating every matrix script.
# Scale_row_count is in millions and auto-scaled to actual.
# Test_iterations is auto-forced to odd.
#
# Usage: build-matrix.sh <matrix-script>

FILE="$1"

if [[ -z "$FILE" ]]; then
  echo "Usage: build-matrix.sh <matrix-script>"
  exit 1
fi

if [[ ! -f "$FILE" ]]; then
  echo "::error::Matrix script not found: ${FILE}"
  exit 1
fi

TSV=$(bash "$FILE")

BAD_FIELDS=$(echo "$TSV" | awk -F'\t' 'NF>0 && NF!=9 {print "  line "NR": "NF" fields"}')
if [[ -n "${BAD_FIELDS}" ]]; then
  echo "::error::Every row needs 9 tab-separated fields in ${FILE}"; echo "${BAD_FIELDS}"; exit 1
fi

# An empty field still counts toward the 9. Worst case is config_options: blank is not <default>,
# so the engine silently starts without -Xmx24g and reports numbers at the wrong heap.
FIELDS="run_type run_label docker_image test_package test_class_list test_iterations scale_row_count distribution config_options"
BAD_EMPTY=$(echo "$TSV" | awk -F'\t' -v names="${FIELDS}" 'BEGIN {split(names, n, " ")}
  NF>0 {for (i=1; i<=NF; i++) if ($i ~ /^[[:space:]]*$/) print "  line "NR": "n[i]" is empty"}')
if [[ -n "${BAD_EMPTY}" ]]; then
  echo "::error::Every field needs a value in ${FILE}. Use <default> for config_options."; echo "${BAD_EMPTY}"; exit 1
fi

# Nightly is excluded on purpose, since a matrix run would collide with that day's real nightly set
BAD_TYPES=$(echo "$TSV" | awk -F'\t' 'NF>0 && $1 !~ /^(adhoc|release|compare)$/ {print "  line "NR": "$1}')
if [[ -n "${BAD_TYPES}" ]]; then
  echo "::error::run_type must be adhoc, release, or compare in ${FILE}"; echo "${BAD_TYPES}"; exit 1
fi

# Rows sharing a run_type and run_label land in one set, and queries report the middle run by rate
BAD_PARITY=$(echo "$TSV" | awk -F'\t' 'NF>0 {c[$1"/"$2]++} END {for (k in c) if (c[k]>1 && c[k]%2==0) print "  "k": "c[k]" rows"}')
if [[ -n "${BAD_PARITY}" ]]; then
  echo "::error::Rows per set must be odd so the reported median is a real run. Add or remove a row in ${FILE}"
  echo "${BAD_PARITY}"; exit 1
fi

RESULT=$(echo "$TSV" | jq -Rsc '
  ["run_type","run_label","docker_image","test_package","test_class_list",
   "test_iterations","scale_row_count","distribution","config_options"] as $h |
  split("\n") | map(select(length > 0)) |
  [.[] | split("\t") | [range(length) as $i | {($h[$i]): .[$i]}] | add |
    if .scale_row_count then
      .scale_row_count = ((.scale_row_count | tonumber) * 1000000 | tostring)
    else . end |
    if .test_iterations then
      .test_iterations = ((.test_iterations | tonumber) as $n |
        if ($n % 2) == 0 then ($n + 1) else $n end | tostring)
    else . end
  ]
')

COUNT=$(echo "$RESULT" | jq 'length')
EXPECTED=$(grep -m1 '^EXPECTED_COMBOS=' "$FILE" | cut -d= -f2 || true)

if [[ -n "$EXPECTED" && "$COUNT" -ne "$EXPECTED" ]]; then
  echo "::error::Expected ${EXPECTED} combinations but got ${COUNT}. Update EXPECTED_COMBOS in ${FILE}."; exit 1
fi
echo "Matrix combinations: ${COUNT}"

MATRIX_OUT=$(echo "$RESULT" | jq -c .)
if [[ -n "$GITHUB_OUTPUT" ]]; then
  echo "matrix=${MATRIX_OUT}" >> "$GITHUB_OUTPUT"
  echo "combo_count=${COUNT}" >> "$GITHUB_OUTPUT"
  echo "### Matrix: ${COUNT} combinations" >> "$GITHUB_STEP_SUMMARY"
else
  echo "matrix=${MATRIX_OUT}"
fi

