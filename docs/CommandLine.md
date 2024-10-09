# Benchmark - Command Line Usage

Users can run Benchmark in an IDE with a standard JUnit test plugin or from the command line.  The Benchmark standalone jar (deephaven-benchmark-1.0-SNAPSHOT-standalone.jar) 
contains all dependencies needed to run the framework.  The standalone console launcher for JUnit is used to run the tests, so all of its command line options are 
available from Benchmark's main jar.

## Examples

Have a look at the available arguments:
```
java -jar deephaven-benchmark-1.0-SNAPSHOT-standalone.jar --help
```

Run tests in a your own jar
```
java -jar deephaven-benchmark-1.0-SNAPSHOT-standalone.jar -cp your-tests.jar -p io.deephaven.your.tests
```

Run tests in your own jar using your own property file
```
java -D"benchmark.profile"="your-benchmark.properties" -jar deephaven-benchmark-1.0-SNAPSHOT-standalone.jar -cp your-tests.jar -p your.tests
```

## Results
The results of running tests in this way are always placed in a *results* directory in the current working directory.  Benchmark
data produced from each run accmulate rather than being overwritten. See [Collected Results](CollectedResults.md)