# Benchmarking Concepts

Benchmark is designed to work easily in different contexts like within an IDE, from the command line, and as part of Github workflows. However, there is more to writing benchmarks than getting a test working and measuring between two points. Is it reproducible? Does it perform similarly at different scales? What about from day to day? Is setup included in the measurement? Is it easy to add to and maintain?

What follows are some concepts that guide the development of Benchmark meant to kept it versatile, simple, and relevant.

### Self-guided API
The *Bench* API uses the builder pattern to guide the test writer in generating data, executing queries, and fetching results. There is a single API entry point where a user can follow the dots and look at the code-insight and Javadocs that pop up in the IDE. Default properties can be overriden by builder-style "with" methods like *withRowCount()*. A middle ground is taken between text configuration and configuration fully-expressed in code to keep things simple and readable.

### Scale Rather Than Iterations
Repeating tests can be useful for testing the effects of caching (e.g. load file multiple times; is it faster on subsequent loads?), or overcoming a lack of precision in OS timers (e.g. run a fast function many times and average), or average out variability between runs (there are always anomalies). On the other hand, if the context of the test is processing large data sets, then it's better to measure against large data sets where possible. This provides a benchmark test that's closer to the real thing when it comes to memory consumption, garbage collection, thread usage, and JIT optimizations. Repeating tests, though useful in some scenarios, can have the effect of taking the operation under test out of the benchmark equation because of cached results, resets for each iteration, limited heap usage, or smaller data sets that are too uniform.

### Adjust Scale For Each Test
When measuring a full set of benchmarks for transforming data, some benchmarks will naturally be faster than others (e.g. sums vs joins). Running all benchmarks at the same scale (e.g. 10 million rows) could yield results where one benchmark takes a minute and another takes 100 milliseconds. Is the 100 ms test meaningful, especially when measured in a JVM? Not really, because there is no time to assess the impact of JVM ergonomics or the effect of OS background tasks. A better way is to set scale multipliers to amplify row count for tests that need it.

### Test-centric Design
Want to know what tables and operations the test uses? Go to the test. Want to know what the framework is doing behind the scenes? Step through the test. Want to run one or more tests? Start from the test rather than configuring an external tool and deploying to that. Let the framework handle the hard part. The point is that a benchmark test against a remote server should be as easy and clear to write as a unit test. As far as is possible, data generation should be defined in the same place it's used... in the test.

### Running in Multiple Contexts
Tests are developed by test-writers, so why not make it easy for them?  Run tests from the IDE for ease of debugging. Point the tests to a local or a remote Deephaven Server instance. Or package tests in a jar and run them locally or remotely from the Benchmark uber-jar. The same tests should work whether running everything on the same system or different system.

### Measure Where It Matters
The Benchmark framework allows the test-writer to set each benchmark measurement from the test code instead of relying on a mechanism that measures automatically behind the scenes. Measurements can be taken across the execution of the test locally with a *Timer* like in the [JoinTablesFromKafkaStreamTest](../src/it/java/io/deephaven/benchmark/tests/internal/examples/stream/JoinTablesFromKafkaStreamTest.java) example test or fetched from the remote Deephaven instance where the test is running as is done in the [StandardTestRunner](../src/it/java/io/deephaven/benchmark/tests/standard/StandardTestRunner.java) used for nightly Deephaven benchmarks. Either way the submission of the result to the Benchmark framework is under the test-writer's control.

### Preserve and Compare
Most benchmarking efforts involve a fixed timeline for "improving performance" rather than tracking the performance impacts of code changes day-to-day. This can be effective, but how do you know if future code changes are degrading performance unless benchmarking is done every day. A better way is to preserve benchmarking results every day and compare to the results from the previous week. To avoid death by a thousand cuts, compare release to release as well for the same benchmarks. Also, compare benchmarks for your product with other products for equivalent operations.

