# Release

This document details the procedure for releasing Benchmark, including publishing the Maven Central artifact and Github Release Notes with assets.

## Prerequisites

- Corresponding released version Deephaven Community Core (DHC)
- DHC docker image used by this release
- Access for running the Publish Release workflow in the main Benchmark repository
- Testing for the relevant combination of Benchmark and DHC image
  - Occurs with nightly Benchmark runs but needs to be done manually in the case of a DHC patch

## Artifacts

### deephaven-benchmark jar

The deephaven-benchmark jar that can be used as a dependency to use the Benchmark API to write tests
It is released to [Maven Central](https://repo1.maven.org/maven2/io/deephaven/).

### deephaven-benchmark tar

The deephaven-benchmark tar distribution that allows users to run pre-existing Deephaven tests from a self-contained directory.
It is attached to the corresponding [Github Release Notes](https://github.com/deephaven/benchmark/releases/).

### The Happy Path (DHC release and patch)
 
Typically, the release process up to and including the Github Release is run from the Github workflow, even for Deephaven Core patches.

- Go to the [Publish Deephaven Benchmark](https://github.com/deephaven/benchmark/actions/workflows/publish-benchmarks.yml) workflow
- Click the "Run Workflow" dropdown on the right-hand side of the screen
- Run only on the _main_ branch
- Fill in the Benchmark release version, which is the same as the DHC version
- Fill in the commit hash for the Benchmark commit to tag as the Benchmark release
- Fill in the previous tag to compare to. This is "previous" in [semver](https://semver.org/) order, not date order
- Click the "Run Workflow" button at the bottom
- When the workflow finishes, look for the release in [Github Release Notes](https://github.com/deephaven/benchmark/releases/)
- Make sure the correct Github Release is labelled, or still labelled, "Latest"
- Download and test the tar asset in the Github release. (Level of testing is informed by DHC and Benchmark release notes.)

> [!Note]
> The [Sonatype Central Portal](https://central.sonatype.com/publishing) can be very slow and requires some patience.

### Benchmark Patch

In the rare case where there needs to be a patch on Benchmark to work with a DHC patch, a branch is created off of the Benchmark main repo. After testing to make sure the patch works, the "Happy Path" above can be followed.

- Verify that upstream is set in a local "benchmark" clone 
  - Running `git remote get-url upstream` should yield `git@github.com:deephaven/benchmark.git`
  - If upstream is not set, use `git remote add upstream git@github.com:deephaven/benchmark.git`
- Make a branch for the new tag 
  - Run `git fetch upstream`
  - Run `git checkout vX.Y` (Use the previous tag for X.Y, ex. `v42.0`)
  - Run `git checkout -b release/vX.Y` (Use the release tag for X.Y, ex. `v42.1`)
  - Make appropriate Benchmark changes and test
  - Run `git push -u upstream release/vX.Y`
  - Run the "Happy Path" above (Do not merge the release branch)

### Publish to Maven Central

One Benchmark artifact is published to [Maven Central](https://repo1.maven.org/maven2/io/deephaven/deephaven-benchmark/) along with javadocs and source files. These have requirements for signing using the [Deephaven Benchmark Certificate](https://keyserver.ubuntu.com/pks/lookup?search=Deephaven+Benchmark&fingerprint=on&op=index). The bundle created by the previous steps does this. Publishing is done through the [Sonatype Central Portal](https://central.sonatype.com/publishing) by [uploading the bundle](https://central.sonatype.org/publish/publish-portal-upload/).

- Download _Deephaven Benchmark Release X.Y_ from the _Publish Deephaven Benchmark_ workflow Artifacts and extract the _deephaven-benchmark-X.Y-bundle.zip_.
- Log in to the [Sonatype Central Portal](https://central.sonatype.com/publishing) and click on "Publish Component"
- Enter a "Deployment Name" (ex. `io.deephaven:deephaven-benchmark:X.Y`), click "Upload File" to select the bundle zip, and click "Publish Component"
- The deployment will show up in the "Deployments" list. Sonatype performs various validation and it may take some time for the status to change from "Validating" to "Validated"
- If there are no errors, and the deployment is "Validated", click "Publish" to release it (or "Drop" to discard it)
- After selecting "Publish", the artifacts will eventually show up in [Maven Central](https://repo1.maven.org/maven2/io/deephaven/deephaven-benchmark/). Verify that the version has landed.

