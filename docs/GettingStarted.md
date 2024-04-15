# Benchmark Getting Started

Benchmark testing requires four parts; JUnit tests using the BenchAPI, an way to compile the tests, a Kafka service, 
and Deephaven Community Core.  (Running on multiple servers is out of the scope of this guide.)

## Assumptions of this Guide

- You already understand Java and how to write Junit5 tests
- You have a working Java IDE that has a JUnit5 plugin (or can build from the command line)
- You know (or can look up) something about docker compose

## Prerequisites for Running Existing Tests

- System Memory: 32G (to run with the default settings)
- CPU Threads: 16 is preferrable (can be run with less, but that may compromise the test execution quality)
- Storage: 10G of free space (some for Deephaven install, some for generated data)

## Easy Installation Of Deephaven and Redpanda

Getting the Deephaven and Redpanda installation is easy.  For Linux distros, you must have the Docker package
installed.  For Windows, you must have Docker Desktop installed. See 
[Deephaven's Quick Start Guide](https://deephaven.io/core/docs/tutorials/quickstart/) for Docker version requirements
and setup tips.  

For Benchmark setup, the following docker-compose.yml file is useful.
````
services:
  deephaven:
    image: ghcr.io/deephaven/server:edge
    ports:
      - "${DEEPHAVEN_PORT:-10000}:10000"
    volumes:
      - ./data:/data
    environment:
      - "START_OPTS=-Xmx24g -DAuthHandlers=io.deephaven.auth.AnonymousAuthenticationHandler"

  redpanda:
    command:
    - redpanda
    - start
    - --smp 2
    - --memory 2G
    - --reserve-memory 0M
    - --overprovisioned
    - --node-id 0
    - --check=false
    - --kafka-addr
    - PLAINTEXT://0.0.0.0:29092,OUTSIDE://0.0.0.0:9092
    - --advertise-kafka-addr
    - PLAINTEXT://redpanda:29092,OUTSIDE://localhost:9092
    - --pandaproxy-addr 0.0.0.0:8082
    - --advertise-pandaproxy-addr redpanda:8082
    image: docker.redpanda.com/vectorized/redpanda:v23.2.22
    ports:
    - 8081:8081
    - 8082:8082
    - 9092:9092
    - 29092:29092
````

## Building/Running Tests Outside of the IDE

After checking out the Benchmark project, running *mvn verify* from the directory containing the pom.xml will compile
the code, package the uber main and test jars, and run some integration tests against your running Deephave Server.

The jar artifacts produce by the build are
- deephaven-benchmark-1.0-SNAPSHOT.jar: The main uber jar
- deephaven-benchmark-1.0-SNAPSHOT-tests.jar: The jar containing project tests

Run the uber jar according to the [Command Line Guide](CommandLine.md)
Results of the benchmark run will be placed in the working directory.

## Running Tests Inside of the IDE

Benchmark is a Maven project that can be imported into popular IDE's like Eclipse and IntelliJ. After the project
is imported and built, navigate to the package *io.deephaven.benchmark.tests.standard* in */src/it/java*. Tests can 
be run individually or as a collection just like any JUnit5 test.

When running from an IDE, the results of the run will be place in the IDE-defined working directory.
