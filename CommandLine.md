# Benchmark - Command Line Usage

Users can run Benchmark in an IDE with a standard JUnit test plugin or from the command line.  The Benchmark artifact (e.g. deephaven-benchmark-1.0-SNAPSHOT.jar) contains all dependencies needed to run the framework.  The standalone console launcher for JUnit is used to run the tests, so all of its command line options are available from Benchmark's main jar.

One difference is that when the command is run without arguments, rather than reporting and error as the Console Launcher does, Benchmark uses its own defaults.

Benchmark Defaults:
- \-p io.deephaven.benchmark.tests		*(Selects tests within the default Benchmark test package)*
- \-n "^\(.\*\)$"					*(Selects all tests within the package regardless of naming-convention)*

## Examples

Have a look at the available arguments:
```
java -jar deephaven-benchmark-1.0-SNAPSHOT.jar --help
```

Run all tests in the Benchmark *tests* package:
```
java -jar deephaven-benchmark-1.0-SNAPSHOT.jar
```

Run tests in a your own jar
```
java -jar deephaven-benchmark-1.0-SNAPSHOT.jar -cp your-tests.jar -p io.deephaven.your.tests
```
