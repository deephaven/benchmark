# Deephaven Benchmark Distribution

The Benchmark distribution is a self-contained mechanism for running existing Deephaven operational benchmarks against Deephaven Community Core (DHC) without the need for checking out the Benchmark project or running from Github workflows.

Prerequisites
- [Benchmark Distribution Tar](https://github.com/deephaven/benchmark/releases/latest/)
- [Docker](https://docs.docker.com/engine/install/)
- Linux Operating System(https://www.linux.com/what-is-linux/)
- [Java 21+](https://adoptium.net/temurin/releases/)

Notes
- Benchmarks are only tested and run on Ubuntu Linux. Other Operating Systems may work but may not be supported
- Nightly benchmarks are run at a base scale of 10mm rows
- Variability amongs Rates between runs for the same benchmark is likely, even on the same hardware
- The base scale for the Benchmark nightly runs is 10 mm rows
- Running all Deephaven benchmarks, like those done every night, takes over 7.5 hours

[!IMPORTANT] 
If other docker containers are running on the same system, there could be conflicts.

# Running the Benchmarks

Each Benchmark release includes a tar asset in the Github release.  This can be downloaded, unpacked into a directory, and run with the provided script.

- Download the Benchmark distribution tar into an empty directory.  ex `curl https://github.com/deephaven/benchmark/releases/download/v0.36.1/deephaven-benchmark-0.36.1.tar`
- From that directory, unpack the tar file. ex `tar xvf deephaven-benchmark-0.36.1.tar`
- Test to make sure things work. ex. `./benchmark 1 Avg*
- When tests are finished, check the results ex `cat results/benchmark-summary-results.csv`

# Benchmarking like Deephaven

If you've gotten this far, you are now using the same software Deephaven uses to run benchmarks on DHC.  However, the configuration of the Benchmark distribution is not necessarily the same as what is used every night.  See the full documentation in Github for more information on Benchmark concepts, configuration, running and more.
