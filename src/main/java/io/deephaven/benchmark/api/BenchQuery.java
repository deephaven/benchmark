/* Copyright (c) 2022-2025 Deephaven Data Labs and Patent Pending */
package io.deephaven.benchmark.api;

import java.io.Closeable;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Function;
import io.deephaven.benchmark.connect.ConnectorFactory;
import io.deephaven.benchmark.connect.ResultTable;
import io.deephaven.benchmark.metric.Metrics;
import io.deephaven.benchmark.util.Timer;

/**
 * Represents a query, its execution, and results. The query logic is executed and the resulting tables subscribed to
 * and data fetched. Table contents can be retrieved with fetchDuring (for ticking tables) and fetchAfter (for
 * snapshots) depending on what type of tables the query initiates.
 */
final public class BenchQuery implements Closeable {
    final Bench bench;
    final String logic;
    final QueryLog queryLog;
    final Map<String, Consumer<ResultTable>> snapshotFetchers = new LinkedHashMap<>();
    final Map<String, Function<ResultTable, Boolean>> tickingFetchers = new LinkedHashMap<>();
    final Properties props = new Properties();
    final Session session;

    BenchQuery(Bench bench, String logic, QueryLog queryLog) {
        this.bench = bench;
        this.logic = logic;
        this.queryLog = queryLog;
        this.session = bench.session;
    }

    /**
     * Subscribes a handler to receive table data after execution of the query logic is completed. (Note: This will not
     * work on ticking tables)
     * 
     * @param table a table name present in the query logic
     * @param tableHandler a consumer that receives a non-live snapshot of the table
     * @return this bench query instance
     */
    public BenchQuery fetchAfter(String table, Consumer<ResultTable> tableHandler) {
        snapshotFetchers.put(table, tableHandler);
        return this;
    }

    /**
     * Subscribes a handler to receive table data during execution of the query logic. The update interval is defined by
     * the session. On each interval, the handler must return true (continue) or false (end/unsubscribe)
     * 
     * @param table a table name present in the query logic
     * @param tableHandler a function that receives non-live snapshot of the table
     * @return this bench query instance
     */
    public BenchQuery fetchDuring(String table, Function<ResultTable, Boolean> tableHandler) {
        tickingFetchers.put(table, tableHandler);
        return this;
    }

    /**
     * Add properties to be passed to the <code>Connector</code> used in the query
     * 
     * @param name the name of the property
     * @param value the value of the property
     * @return this bench query instance
     */
    public BenchQuery withProperty(String name, String value) {
        if (session.getConnector() != null) {
            throw new RuntimeException("Cannot set properties after first query is executed");
        }
        if (value != null && !value.isBlank()) {
            props.setProperty(name, value);
        }
        return this;
    }

    /**
     * Execute the query logic through a session
     */
    public void execute() {
        var timer = Timer.start();
        executeBarrageQuery(logic);
        var conn = session.getConnector();
        tickingFetchers.entrySet().forEach(e -> bench.addFuture(conn.fetchTickingData(e.getKey(), e.getValue())));

        snapshotFetchers.entrySet().forEach(e -> {
            try {
                Future<Metrics> f = conn.fetchSnapshotData(e.getKey(), e.getValue());
                Metrics metrics = f.get();
                metrics.set("duration.secs", timer.duration().toMillis() / 1000.0);
                bench.addFuture(f);
            } catch (Exception ex) {
                throw new RuntimeException("Failed to get snapshot of table: " + e.getKey(), ex);
            }
        });
    }

    /**
     * Unsubscribe any fetchers, free used variables, and close the session
     */
    public void close() {
        var conn = session.getConnector();
        if (conn == null)
            return;

        if (!conn.getUsedVariableNames().isEmpty()) {
            String logic = String.join("=None; ", conn.getUsedVariableNames()) + "=None\n";
            executeBarrageQuery(logic);
        }
    }

    // Add function defs in separate query so if there are errors in the "logic" part, the line numbers match up
    private void executeBarrageQuery(String logic) {
        var conn = session.getConnector();
        if (conn == null) {
            var connectorClass = bench.property("connector.class", "io.deephaven.benchmark.connect.BarrageConnector");
            var localProps = Bench.profile.getProperties();
            localProps.putAll(props);
            conn = ConnectorFactory.create(connectorClass, localProps);
            session.setConnector(conn);
        }
        String snippetsLogic = Bench.profile.replaceProperties(Snippets.getFunctions(logic));
        if (!snippetsLogic.isBlank()) {
            queryLog.logQuery(snippetsLogic);
            conn.executeQuery(snippetsLogic);
        }
        String userLogic = Bench.profile.replaceProperties(logic);
        conn.executeQuery(userLogic);
        queryLog.logQuery(userLogic);
    }

}
