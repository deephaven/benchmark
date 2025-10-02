#!/usr/bin/env bash

set -o errexit
set -o pipefail
set -o nounset

# Copyright (c) 2024-2024 Deephaven Data Labs and Patent Pending

# Create a bundle suitable for uploading artifacts to oss.sonatype including pom, sources, javadoc, 
# artifact and signatures for each. This script expects to run in the directory where the 
# release branch has been checked out
#
# ex. .github/scripts/make-release-artifact-bundle.sh 0.33.4 keyfile.asc

if [[ $# != 2 ]]; then
    echo "$0: Missing release version or signature file argument"
    exit 1
fi

VERSION=$1
KEY_FILE=$2
ARTIFACT=deephaven-benchmark-${VERSION}
TARGET=io/deephaven/deephaven-benchmark/${VERSION}

sed -i "s;<version>1.0-SNAPSHOT</version>;<version>${VERSION}</version>;" pom.xml
mvn -B install --file pom.xml

cp pom.xml ${ARTIFACT}.pom
cp target/${ARTIFACT}.jar .
cp target/${ARTIFACT}-sources.jar .
cp target/${ARTIFACT}-javadoc.jar .

gpg --import ${KEY_FILE}

sign() {
  gpg --batch --yes -ab $1
  shasum -a 1 "$1" | cut -d ' ' -f 1 > "$1.sha1"
  md5sum "$1" | cut -d ' ' -f 1 > "$1.md5"
}

sign ${ARTIFACT}.pom
sign ${ARTIFACT}.jar
sign ${ARTIFACT}-javadoc.jar
sign ${ARTIFACT}-sources.jar

mkdir -p ${TARGET}
cp ${ARTIFACT}.* ${TARGET}
cp ${ARTIFACT}-* ${TARGET}

zip -r ../${ARTIFACT}-bundle.zip io/

