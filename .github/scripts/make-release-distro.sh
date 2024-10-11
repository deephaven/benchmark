#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Create a tar file with the given version using the git project located in the 
# working directory.

if [[ $# != 2 ]]; then
    echo "$0: Missing release version or distro source argument"
    exit 1
fi

RELEASE_VERSION=$1
DISTRO_SOURCE=$2
ARTIFACT=deephaven-benchmark-${RELEASE_VERSION}
DISTRO_DEST=target/distro
WORKING_DIR=$(pwd)

# Generate dependencies directory
mkdir -p ${DISTRO_DEST}/libs/
cd ${DISTRO_DEST}
cat ${DISTRO_SOURCE}/dependency-pom.xml | sed "s!<version>1.0-SNAPSHOT</version>!<version>${RELEASE_VERSION}</version>!g" > dependency-pom.xml
mvn -B install --file dependency-pom.xml
mv target/dependencies/* libs/
rm -rf target
rm libs/${ARTIFACT}*.jar
cd ${WORKING_DIR}

# Build the Distro for running standard benchmarks
cp ${DISTRO_SOURCE}/* ${DISTRO_DEST}
rm ${DISTRO_DEST}/dependency-pom.xml
cp target/${ARTIFACT}.jar ${DISTRO_DEST}/libs/
cp target/${ARTIFACT}-tests.jar ${DISTRO_DEST}/libs/
echo "VERSION=${RELEASE_VERSION}" > ${DISTRO_DEST}/.env

cd ${DISTRO_DEST}
tar cvzf ../${ARTIFACT}.tar * .env

