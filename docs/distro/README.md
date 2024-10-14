# Deephaven Benchmark Distribution

The Benchmark distribution is a self-contained mechanism for running existing Deephaven operational benchmarks against Deephaven Community Core (DHC) without the need for checking out the Benchmark project or running from Github workflows.

Prerequisites
- [Benchmark Distribution Tar](https://github.com/deephaven/benchmark/releases/latest/)
- [Docker](https://docs.docker.com/engine/install/)
- [Linux Operating System](https://www.linux.com/what-is-linux/)
- [Java 21+](https://adoptium.net/temurin/releases/)

Notes
- Benchmarks are only tested and run on [Ubuntu Linux](https://ubuntu.com/server). Other Operating Systems may work but may not be supported
- Variability for Rates between runs for the same benchmark is likely, even on the same hardware
- The base scale for Deephaven's nightly Benchmark runs is 10mm rows
- Running all Deephaven benchmarks, like those done every night, takes over 7.5 hours

> [!WARNING]   
> If other docker containers are running on the same system, there may be conflicts.

## Running the Benchmarks

Each Benchmark release includes a tar asset in the [Github Releases](https://github.com/deephaven/benchmark/releases).  This can be downloaded, unpacked into a directory, and run with the provided script.

- Download the Benchmark distribution tar into an empty directory.  ex. `wget https://github.com/deephaven/benchmark/releases/download/v0.36.1/deephaven-benchmark-0.36.1.tar`
- From that directory, unpack the tar file. ex. `tar xvf deephaven-benchmark-0.36.1.tar`
- Test to make sure things work. ex. `./benchmark 1 Avg*`
- When the tests are finished, check the results. ex. `cat results/benchmark-summary-results.csv`
- Try running the same set as before at higher scale and more iterations
  - In *benchmark.properties*, overwrite exsiting row count with `scale.row.count=10000000`
  - Run the script again.  It will take much longer. ex. `./benchmark 3 Avg*`
  - The results will contain 3 runs, each with a result csv
- If the host system has enough memory, try increasing DHC memory and runnning at even higher scale
  - Edit *docker-compose.yml* and change `-Xmx24G` to `-Xmx48G`
  - In *benchmark.properties*, set `scale.row.count` higher
  
> [!WARNING]  
> Setting `scale.row.count` to a higher value will effect memory usage in DHC.  If set too high, DHC may crash with an "Out of Memory" error.

## Benchmarking like Deephaven

If you've gotten this far, you are now using the same software Deephaven uses to benchmark DHC.  However, the configuration of the Benchmark distribution is not necessarily the same as what is used every night.  It is also difficult to reproduce the same Rates without running on exactly the same hardware.  So expect differences. See the [full documentation in Github](https://github.com/deephaven/benchmark) for more information on Benchmark concepts, configuration, running, recent results, and more.

> [!NOTE]  
> More information on Deephaven Community Core and Deephaven Enterprise can be found at [Deephaven IO](https://deephaven.io/)





