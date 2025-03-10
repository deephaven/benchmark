/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.connect;

import java.util.Set;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import io.deephaven.benchmark.metric.Metrics;

/**
 * An Object that provides a connection to a service under test, execution of statements, and retrieval of results
 * through snapshots or live data. What this all means is up to the implementer, but it's execution is supported by
 * Benchmark API in {@link io.deephaven.benchmark.api.BenchQuery}.
 */
public interface Connector extends AutoCloseable {

    /**
     * Execute a query or statement against the connected service
     * 
     * @param query the statement to be executed
     */
    public void executeQuery(String query);

    /**
     * Get a list a variable or table names used in the query. This is optional and may return an empty set.
     * 
     * @return a set of variable names or empty set
     */
    public Set<String> getUsedVariableNames();

    /**
     * Fetch data as a snapshot of the given table and return a result table according to the given consumer. This fetch
     * is made after the execution of the query.
     * 
     * @param table the table to fetch data from
     * @param tableHandler a consumer to supplied to receive the fetched data
     * @return a future that waits until the fetch is complete
     */
    public Future<Metrics> fetchSnapshotData(String table, Consumer<ResultTable> tableHandler);

    /**
     * Fetch data for the given table as it is updated while the query is running. The given function is called at a
     * refresh rate determined by the <code>Connector</code> implementation.
     * 
     * @param table the table to fetch data from
     * @param tableHandler a function to call on a cycle
     * @return a future that waits until the fetch initiates
     */
    public Future<Metrics> fetchTickingData(String table, Function<ResultTable, Boolean> tableHandler);

    /**
     * Close the connector and clean up resources
     */
    public void close();

}
